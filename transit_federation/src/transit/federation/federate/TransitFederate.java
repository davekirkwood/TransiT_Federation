package transit.federation.federate;

import java.nio.charset.StandardCharsets;

import hla.rti1516e.CallbackModel;
import hla.rti1516e.FederateAmbassador.SupplementalReceiveInfo;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.TransportationTypeHandle;

public abstract class TransitFederate {

	protected static final int TOTAL_FEDERATES = 3;

	protected static final String FEDERATION_NAME = "TransiTFederation";

	private static int stillJoined = TOTAL_FEDERATES;

	protected final String federateName;

	protected RTIambassador rtiamb;
	
	protected InteractionClassHandle timeTickHandle;
	protected ParameterHandle timeHandle;

	protected InteractionClassHandle aMessageHandle;
	protected ParameterHandle textHandle;

	protected InteractionClassHandle stopHandle;

	protected volatile boolean running = false;
	private volatile boolean joined = false;

	protected TransitFederate(String federateName) {
		this.federateName = federateName;
	}

	public void start() throws Exception {

		rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		rtiamb.connect(new TransitFederateAmbassador(this), CallbackModel.HLA_IMMEDIATE);
		rtiamb.joinFederationExecution(federateName, "SimpleFederateType", FEDERATION_NAME);

		joined = true;
		running = true;
		
		configure();

		System.out.println(federateName + " joined and configured.");

	}

	protected abstract void configure() throws Exception;



	public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, byte[] tag, OrderType sentOrdering, TransportationTypeHandle transport,
			SupplementalReceiveInfo receiveInfo) {

		String sender = tag == null ? "" : new String(tag, StandardCharsets.UTF_8);
		// Ignore our own TimeTicks
		if (sender.equals(federateName)) {
			return;
		}
		receiveApplicationInteraction(interactionClass, parameters, sender);
	}

	protected abstract void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender);

	public void stop() throws Exception {

		running = false;

		if (rtiamb != null) {
			if (joined) {
				rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
				joined = false;
				stillJoined--;
				System.out.println(federateName + " resigned. Still joined: " + stillJoined);
			}

			rtiamb.disconnect();
		}

		System.out.println(federateName + " stopped.");
	}

	public static int getStillJoined() {
		return stillJoined;
	}




	public void runUntilStopped() throws Exception {
		start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		// Keep the application alive
		Thread.currentThread().join();
	}
}