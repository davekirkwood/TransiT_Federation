package transit.federation.simfederate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hla.rti1516e.*;
import transit.federation.federate.TransitFederate;

public class MATSimFederate extends TransitFederate {

	public static final String MATSIM_NAME = "MATSim";
	private InteractionClassHandle federateShutdown;

	private Thread tickThread;
	
	private int replies = 0;		// TODO Change to sync on received times, rather than reply count

	public MATSimFederate() {
		super(MATSIM_NAME);
	}

	@Override
	protected void configure() throws Exception {
		super.configure();
		/*
		 * MATSim Timeticks (Publish)
		 */
		handle_timeTick = rtiamb.getInteractionClassHandle("TimeTick");
		handle_time = rtiamb.getParameterHandle(handle_timeTick, "Time");
		rtiamb.publishInteractionClass(handle_timeTick);
		System.out.println("MATSim publishing TimeTick");

		rtiamb.subscribeInteractionClass(handle_timeTick);

		/*
		 * Stop Simulation (Send)
		 */
		handle_stop = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.publishInteractionClass(handle_stop);

		/*
		 * Federate Shutdown (Send)
		 */
		federateShutdown = rtiamb.getInteractionClassHandle("FederateShutdown");
		rtiamb.publishInteractionClass(federateShutdown);

//		/*
//		 * Test Message (Publish)
//		 */
//		aMessageHandle = rtiamb.getInteractionClassHandle("A_Message");
//		textHandle = rtiamb.getParameterHandle(aMessageHandle, "Text");
//		rtiamb.publishInteractionClass(aMessageHandle);
//		System.out.println("MATSim publishing A_Message");

	}

	public void simulate() {
		tickThread = new Thread(() -> {
			double simulationTime = 0.0;
			while (running) {
				try {
					if (simulationTime >= 10.0) {
						sendStopMessage();
						sendFederateShutdown();
						stop();
						break;
					} else {
						sendTimeTick(simulationTime);
						replies = 0;
//						sendTestMessage();
						simulationTime += 1.0;
						Thread.sleep(1000);
						while(replies < otherFederates.size()) {
							Thread.sleep(10);
						}
					}
				} catch (InterruptedException e) {
					break;

				} catch (Exception e) {
					if (running) {
						e.printStackTrace();
					}
					break;
				}
			}
		}, federateName + "-TimeTick");
		tickThread.start();
	}

	/**
	 * Send FederateShutdown interaction
	 * 
	 * @throws Exception
	 */
	private void sendFederateShutdown() throws Exception {
		rtiamb.sendInteraction(federateShutdown, rtiamb.getParameterHandleValueMapFactory().create(1), federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT FederateShutdown");
	}

//	/**
//	 * Send Test message
//	 * 
//	 * @throws Exception
//	 */
//	public void sendTestMessage() throws Exception {
//
//		String message = "Hello BRaVE, this is MATSim!";
//		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
//		parameters.put(textHandle, message.getBytes(StandardCharsets.US_ASCII));
//		rtiamb.sendInteraction(aMessageHandle, parameters, new byte[0]);
//		System.out.println("MATSim SENT A_Message: " + message);
//	}


	private void sendStopMessage() throws Exception {
		rtiamb.sendInteraction(handle_stop, rtiamb.getParameterHandleValueMapFactory().create(1), federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT StopSimulation");
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
			replies++;
		} else {
			System.err.println(federateName + " RECEIVED Unknown interaction " + interactionClass);
		}
	}

	public void stop() throws Exception {
		super.stop();
	}

	public static void main(String[] args) throws Exception {

		MATSimFederate matsim = new MATSimFederate();
		matsim.start();
		matsim.simulate();

	}

}