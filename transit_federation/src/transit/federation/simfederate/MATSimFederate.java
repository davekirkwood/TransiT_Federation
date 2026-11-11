package transit.federation.simfederate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import hla.rti1516e.*;
import transit.federation.federate.TransitFederate;

public class MATSimFederate extends TransitFederate {

	private Thread tickThread;

	public MATSimFederate() {
		super("MATSim");
	}

	@Override
	protected void configure() throws Exception {

		/*
		 * MATSim Timeticks (Publish)
		 */
		timeTickHandle = rtiamb.getInteractionClassHandle("TimeTick");
		timeHandle = rtiamb.getParameterHandle(timeTickHandle, "Time");
		rtiamb.publishInteractionClass(timeTickHandle);
		System.out.println("MATSim publishing TimeTick");

//		rtiamb.subscribeInteractionClass(timeTickHandle);

		/*
		 * Stop Simulation (Send)
		 */
		stopHandle = rtiamb.getInteractionClassHandle("StopSimulation");
		rtiamb.publishInteractionClass(stopHandle);
		
		/*
		 * Test Message (Publish)
		 */
		aMessageHandle = rtiamb.getInteractionClassHandle("A_Message");
		textHandle = rtiamb.getParameterHandle(aMessageHandle, "Text");
		rtiamb.publishInteractionClass(aMessageHandle);
		System.out.println("MATSim publishing A_Message");

	}

	public void simulate() {
		tickThread = new Thread(() -> {
			double simulationTime = 0.0;
			while (running) {
				try {
					if (simulationTime >= 10.0) {
						sendStopMessage();
						stop();
						break;
					} else {
						sendTimeTick(simulationTime);
						sendTestMessage();
						simulationTime += 1.0;
						Thread.sleep(1000);
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
	 * Send Test message
	 * 
	 * @throws Exception
	 */
	public void sendTestMessage() throws Exception {

		String message = "Hello BRaVE, this is MATSim!";
		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
		parameters.put(textHandle, message.getBytes(StandardCharsets.US_ASCII));
		rtiamb.sendInteraction(aMessageHandle, parameters, new byte[0]);
		System.out.println("MATSim SENT A_Message: " + message);
	}

	/**
	 * Send a time tick
	 * 
	 * @param time
	 * @throws Exception
	 */
	protected void sendTimeTick(double time) throws Exception {

		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);
		byte[] encodedTime = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(time).array();
		parameters.put(timeHandle, encodedTime);
		rtiamb.sendInteraction(timeTickHandle, parameters, federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT TimeTick: " + time);
	}

	private void sendStopMessage() throws Exception {
		rtiamb.sendInteraction(stopHandle, rtiamb.getParameterHandleValueMapFactory().create(1), federateName.getBytes(StandardCharsets.UTF_8));
		System.out.println(federateName + " SENT StopSimulation");
	}

	@Override
	protected void receiveApplicationInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, String sender) {
		// TODO Auto-generated method stub

	}

	public void stop() throws Exception {
		super.stop();
	}

	public static void main(String[] args) throws Exception {

		MATSimFederate matsim = new MATSimFederate();
		matsim.start();
		matsim.simulate();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				matsim.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		// Give BRaVE time to join and subscribe.
		Thread.sleep(3000);

		Thread.currentThread().join();

	}

}