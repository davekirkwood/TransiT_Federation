package transit.federation.simfederate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;
import transit.federation.federate.TransitFederate;

public class BRaVEFederate extends TransitFederate {
	
	public BRaVEFederate() {
		super("BRaVE");
	}

	@Override
	protected void configure() throws Exception {
		super.configure();
		
		handle_timeTick = rtiamb.getInteractionClassHandle("TimeTick");
		handle_time = rtiamb.getParameterHandle(handle_timeTick, "Time");

		rtiamb.publishInteractionClass(handle_timeTick);
		rtiamb.subscribeInteractionClass(handle_timeTick);
//
//		aMessageHandle = rtiamb.getInteractionClassHandle("A_Message");
//		textHandle = rtiamb.getParameterHandle(aMessageHandle, "Text");

//		rtiamb.subscribeInteractionClass(aMessageHandle);

		handle_stop = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.subscribeInteractionClass(handle_stop);

	}

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
//		} else if (interactionClass.equals(aMessageHandle)) {
//			byte[] value = parameters.get(textHandle);
//			if (value == null) {
//				return;
//			}
//			String message = new String(value, StandardCharsets.US_ASCII);
//			System.out.println("BRaVE RECEIVED A_Message: " + message);
		} else if (interactionClass.equals(handle_stop)) {
			System.out.println("BRaVE RECEIVED StopSimulation");
			try {
				stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println(federateName + " RECEIVED Unknown interaction " + interactionClass);
		}
		
		
	}
	
	public static void main(String[] args) throws Exception {
		new BRaVEFederate().start();
	}
}