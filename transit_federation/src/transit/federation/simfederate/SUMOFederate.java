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
		super.configure();
		handle_timeTick = rtiamb.getInteractionClassHandle("TimeTick");
		handle_time = rtiamb.getParameterHandle(handle_timeTick, "Time");

		rtiamb.publishInteractionClass(handle_timeTick);
		rtiamb.subscribeInteractionClass(handle_timeTick);
		
		handle_stop = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.subscribeInteractionClass(handle_stop);

		federateShutdownHandle = rtiamb.getInteractionClassHandle("FederateShutdown");
		rtiamb.publishInteractionClass(federateShutdownHandle);
	}

	private InteractionClassHandle federateShutdownHandle;

	@Override
	protected void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender) {
		if (interactionClass.equals(handle_timeTick)) {

			byte[] value = parameters.get(handle_time);

			if (value == null || value.length != 8) {
				System.err.println("Invalid TimeTick parameter");
				return;
			}
			double time = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getDouble();
			System.out.println(federateName + " RECEIVED TimeTick " + time + " from " + sender);
			if(sender.equals(MATSimFederate.MATSIM_NAME)) {	
				try {
					sendTimeTick(time);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (interactionClass.equals(handle_stop)) {
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