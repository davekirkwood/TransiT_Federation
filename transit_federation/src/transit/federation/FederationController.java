
package transit.federation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.portico2.common.configuration.RID;
import org.portico2.rti.RTI;

import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import transit.federation.federate.TransitFederate;
import transit.federation.federate.TransitFederateAmbassador;

public class FederationController extends TransitFederate {

	private static final String FEDERATION_NAME = "TransiTFederation";

	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);


	RTI porticoServer;

	private List<String> federates = new ArrayList<>();

	public FederationController() {
		super("Controller");
	}

	@Override
	public void start() throws Exception {

		System.out.println("Starting Portico RTI...");

//        org.portico.Main.main(new String[] { "rti" });

		RID rid = RID.loadRid(new String[0]);

		porticoServer = new RTI(rid);
		porticoServer.startup();

		System.out.println("Portico RTI started.");

		rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();

		rtiamb.connect(new TransitFederateAmbassador(this), CallbackModel.HLA_IMMEDIATE);

		URL fom = getClass().getResource("/TransiT.fom.xml");

		if (fom == null) {
			throw new IllegalStateException("Cannot find fom");
		}

		try {
			rtiamb.createFederationExecution(FEDERATION_NAME, fom);
			System.out.println("Federation created.");

		} catch (FederationExecutionAlreadyExists e) {
			System.out.println("Federation already exists.");
		}

		// Controller joins as a monitoring federate
		rtiamb.joinFederationExecution(federateName, "SimpleFederateType", FEDERATION_NAME);

		joinedController = true;

		configure();

		System.out.println("Controller joined federation.");
		System.out.println("Waiting for StopSimulation...");
	}

	private volatile boolean joinedController = false;

	@Override
	protected void configure() throws Exception {
		super.configure();
		rtiamb.subscribeInteractionClass(handle_join);

		handle_stop = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.subscribeInteractionClass(handle_stop);

		handle_status = rtiamb.getInteractionClassHandle("Status");
		handle_status_federates = rtiamb.getParameterHandle(handle_status, "Federates");
		rtiamb.publishInteractionClass(handle_status);
		
		System.out.println("Controller subscribed to StopSimulation.");
	}

	@Override
	protected void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender) {
		if (interactionClass.equals(handle_join)) {

			try {
				decoder.decode(parameters.get(handle_join_federateName));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String federateName = decoder.getValue();

			System.out.println("Controller detected join: " + federateName);
			federates.add(federateName);
			
			String fcsv = "";
			for(String f : federates) {
				fcsv += (fcsv.length() > 0 ? "," : "") + f;
			}
			try {
				sendStringInteraction(handle_status, handle_status_federates, fcsv);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (interactionClass.equals(handle_stop)) {

			System.out.println("Controller RECEIVED StopSimulation");

			// Only initiate shutdown once
			if (shuttingDown.compareAndSet(false, true)) {

				Thread shutdownThread = new Thread(() -> {
					try {
						shutdown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}, "FederationController-Shutdown");

				shutdownThread.start();
			}
		}
	}

	private void shutdown() throws Exception {

		System.out.println("Controller shutting down...");

		// Controller must leave the federation first
		if (joinedController) {

			rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);

			joinedController = false;

			System.out.println("Controller resigned.");
		}

		// Wait for remaining federates to resign
		destroyWhenEmpty();

		// Disconnect after federation destruction
		rtiamb.disconnect();

		System.out.println("Controller disconnected.");
		System.out.println("Federation shutdown complete.");
		porticoServer.shutdown();

	}

	private void destroyWhenEmpty() throws Exception {

		while (true) {

			try {
				rtiamb.destroyFederationExecution(FEDERATION_NAME);

				System.out.println("Federation destroyed.");
				return;

			} catch (FederatesCurrentlyJoined e) {

				System.out.println("Waiting for remaining federates to resign...");

				Thread.sleep(500);
			}
		}
	}

	public static void main(String[] args) throws Exception {

		FederationController controller = new FederationController();
		controller.start();
	}
}
