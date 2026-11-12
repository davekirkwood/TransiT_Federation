package transit.federation.simfederate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;
import transit.federation.federate.TransitFederate;

public class SUMOFederate extends TransitFederate {

	public SUMOFederate() {
		super("SUMO");
	}

	@Override
	protected void configure() throws Exception {
		timeTickHandle = rtiamb.getInteractionClassHandle("TimeTick");
		timeHandle = rtiamb.getParameterHandle(timeTickHandle, "Time");

		rtiamb.publishInteractionClass(timeTickHandle);
		rtiamb.subscribeInteractionClass(timeTickHandle);
		
		stopHandle = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.subscribeInteractionClass(stopHandle);

		federateShutdownHandle = rtiamb.getInteractionClassHandle("FederateShutdown");
		rtiamb.publishInteractionClass(federateShutdownHandle);
	}

	private InteractionClassHandle federateShutdownHandle;

	@Override
	protected void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender) {
		if (interactionClass.equals(timeTickHandle)) {

			byte[] value = parameters.get(timeHandle);

			if (value == null || value.length != 8) {
				System.err.println("Invalid TimeTick parameter");
				return;
			}
			double time = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getDouble();

			System.out.println(federateName + " RECEIVED TimeTick " + time + " from " + sender);
		} else if (interactionClass.equals(stopHandle)) {
			System.out.println("SUMO RECEIVED StopSimulation");
			try {
				stop();
				sendFederateShutdown();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println(federateName + " RECEIVED Unknown interaction " + interactionClass);
		}
	}

	private void sendFederateShutdown() throws Exception {
		rtiamb.sendInteraction(federateShutdownHandle, rtiamb.getParameterHandleValueMapFactory().create(1), federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT FederateShutdown");
	}

	public static void main(String[] args) throws Exception {
		new SUMOFederate().start();
	}
}