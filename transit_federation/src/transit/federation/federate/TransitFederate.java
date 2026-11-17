package transit.federation.federate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAASCIIstring;

public abstract class TransitFederate {

	protected static final String FEDERATION_NAME = "TransiTFederation";

	protected final String federateName;

	protected List<String> otherFederates = new ArrayList<>();
	
	protected RTIambassador rtiamb;

	/*
	 * All -> Controller
	 */
	protected InteractionClassHandle handle_join;
	protected ParameterHandle handle_join_federateName;

	/*
	 * Controller -> All
	 */
	protected InteractionClassHandle handle_status;
	protected ParameterHandle handle_status_federates;
	
	
	/*
	 * @formatter:off
	 * MATSim -> BRaVE + SUMO; 
	 * BRaVE -> MATSim; 
	 * SUMO -> MATSim
	 * @formatter:on
	 */
	protected InteractionClassHandle handle_timeTick;
	protected ParameterHandle handle_time;

	/*
	 * MATSim -> BRaVE + SUMO BRaVE + SUMO -> All (Acceptance)
	 */
	protected InteractionClassHandle handle_agentLegStart;
	protected ParameterHandle handle_agentLegStart_agentId;
	protected ParameterHandle handle_agentLegStart_legType;
	protected ParameterHandle handle_agentLegStart_originLocation;
	protected ParameterHandle handle_agentLegStart_destinationLocation;

	/*
	 * BRaVE + SUMO -> MATSim
	 */
	protected InteractionClassHandle handle_agentDeparture;
	protected ParameterHandle handle_agentDeparture_agentId;
	protected ParameterHandle handle_agentDeparture_trainId;

	/*
	 * BRaVE + SUMO -> MATSim
	 */
	protected InteractionClassHandle handle_agentArrival;
	protected ParameterHandle handle_agentArrival_agentId;
	protected ParameterHandle handle_agentArrival_trainId;

	/*
	 * BRaVE -> MATSim
	 */
	protected InteractionClassHandle handle_trainDeparture;
	protected ParameterHandle handle_trainDeparture_trainId;
	protected ParameterHandle handle_trainDeparture_stopId;

	/*
	 * BRaVE -> MATSim
	 */
	protected InteractionClassHandle handle_trainArrival;
	protected ParameterHandle handle_trainArrival_trainId;
	protected ParameterHandle handle_trainArrival_stopId;

	protected InteractionClassHandle handle_stop;

	protected volatile boolean running = false;
	private volatile boolean joined = false;

	protected HLAASCIIstring decoder;

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
		sendFederateJoin();
	}

	protected void configure() throws Exception {
		decoder = RtiFactoryFactory.getRtiFactory().getEncoderFactory().createHLAASCIIstring();

		handle_join = rtiamb.getInteractionClassHandle("Join");
		handle_join_federateName = rtiamb.getParameterHandle(handle_join, "FederateName");
		rtiamb.publishInteractionClass(handle_join);
		
		handle_status = rtiamb.getInteractionClassHandle("Status");
		handle_status_federates = rtiamb.getParameterHandle(handle_status, "Federates");
		rtiamb.subscribeInteractionClass(handle_status);
	}

	/**
	 *
	 * @throws Exception
	 */
	private void sendFederateJoin() throws Exception {
		sendStringInteraction(handle_join, handle_join_federateName, federateName);
		System.out.println(federateName + " SENT Join");
	}

	/**
	 * Helper method to send a message with a string parameter.
	 * @param interaction
	 * @param parameter
	 * @param value
	 * @throws Exception
	 */
	protected void sendStringInteraction(InteractionClassHandle interaction, ParameterHandle parameter, String value) throws Exception {
		HLAASCIIstring encoded = RtiFactoryFactory.getRtiFactory().getEncoderFactory().createHLAASCIIstring(value);
		ParameterHandleValueMap params = rtiamb.getParameterHandleValueMapFactory().create(1);
		params.put(parameter, encoded.toByteArray());
		rtiamb.sendInteraction(interaction, params, federateName.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Send a time tick
	 * 
	 * @param handle_time
	 * @throws Exception
	 */
	protected void sendTimeTick(double t) throws Exception {

		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
		byte[] encodedTime = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(t).array();
		parameters.put(handle_time, encodedTime);
		rtiamb.sendInteraction(handle_timeTick, parameters, federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT TimeTick: " + t);
	}


	public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, byte[] tag, OrderType sentOrdering, TransportationTypeHandle transport,
			SupplementalReceiveInfo receiveInfo) {

		String sender = tag == null ? "" : new String(tag, StandardCharsets.UTF_8);
		// Ignore our own TimeTicks
		if (sender.equals(federateName)) {
			return;
		}
		
		if (interactionClass.equals(handle_status)) {

			try {
				decoder.decode(parameters.get(handle_status_federates));
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String federates = decoder.getValue();
			System.out.println(federateName + " RECEIVED Status" + federates);
			otherFederates = new ArrayList<>();
			for(String f : federates.split(",")) {
				if(!f.equals(federateName)) {
					otherFederates.add(f);
				}
			}
		} else {
			receiveApplicationInteraction(interactionClass, parameters, sender);
		}
	}

	protected abstract void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender);

	public void stop() throws Exception {

		running = false;

		if (rtiamb != null) {
			if (joined) {
				rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
				joined = false;
				System.out.println(federateName + " resigned.");
			}

			rtiamb.disconnect();
		}

		System.out.println(federateName + " stopped.");
	}

}