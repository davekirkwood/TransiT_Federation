package transit.federation.simfederate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import transit.federation.federate.TransitFederate;

public class BRaVEFederate extends TransitFederate {
	
	public BRaVEFederate() {
		super("BRaVE");
	}

	@Override
	protected void configure() throws Exception {

		timeTickHandle = rtiamb.getInteractionClassHandle("TimeTick");
		timeHandle = rtiamb.getParameterHandle(timeTickHandle, "Time");

		rtiamb.publishInteractionClass(timeTickHandle);
		rtiamb.subscribeInteractionClass(timeTickHandle);

		aMessageHandle = rtiamb.getInteractionClassHandle("A_Message");
		textHandle = rtiamb.getParameterHandle(aMessageHandle, "Text");

		rtiamb.subscribeInteractionClass(aMessageHandle);

		stopHandle = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.subscribeInteractionClass(stopHandle);

	}

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
		} else if (interactionClass.equals(aMessageHandle)) {
			byte[] value = parameters.get(textHandle);
			if (value == null) {
				return;
			}
			String message = new String(value, StandardCharsets.US_ASCII);
			System.out.println("BRaVE RECEIVED A_Message: " + message);
		} else if (interactionClass.equals(stopHandle)) {
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
		new BRaVEFederate().runUntilStopped();
	}
}