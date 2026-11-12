package transit.federation;

import java.net.URL;

import hla.rti1516e.CallbackModel;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;

public class FederationController {

	private static final String FEDERATION_NAME = "TransiTFederation";

	private RTIambassador rtiamb;

	public void start() throws Exception {

		// 1. Start the Portico RTI
		System.out.println("Starting Portico RTI...");
		org.portico2.rti.Main.main(new String[0]);

		// 2. Create an RTI ambassador for administration
		rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();

		rtiamb.connect(new NullFederateAmbassador(), CallbackModel.HLA_IMMEDIATE);

		// 3. Load the FOM
		URL fom = getClass().getResource("/Simple.fom.xml");

		if (fom == null) {
			throw new IllegalStateException("Cannot find Simple.fom.xml on classpath");
		}

		// 4. Create the federation
		try {
			rtiamb.createFederationExecution(FEDERATION_NAME, fom);
			rtiamb.disconnect();
			System.out.println("Federation created: " + FEDERATION_NAME);

		} catch (FederationExecutionAlreadyExists e) {
			System.out.println("Federation already exists.");
		}

		// Keep connection open - federates will connect later
		System.out.println("Portico RTI is running.");
		System.out.println("Federation: " + FEDERATION_NAME);
		System.out.println("Waiting for federates to connect...");
	}

	public void stop() throws Exception {

		System.out.println("Stopping federation...");

		// Wait a moment to allow federates to finish resigning and the RTI
		// to settle its multicast state. This prevents the "Async Messages no
		// longer supported" error that occurs when the RTI tries to send
		// DeleteObject via a connection that's in an inconsistent state.
		System.out.println("Waiting for federates to fully resign...");
		Thread.sleep(3000);

		// Now all federates should have resigned - safely destroy the federation
		try {
			rtiamb.connect(new NullFederateAmbassador(), CallbackModel.HLA_IMMEDIATE);
			rtiamb.destroyFederationExecution(FEDERATION_NAME);
			System.out.println("Federation destroyed.");
		} catch (FederatesCurrentlyJoined e) {
			System.out.println("Cannot destroy federation: federates are still joined.");
		} catch (Exception e) {
			System.out.println("Error destroying federation: " + e.getMessage());
		}

		rtiamb.disconnect();

		System.out.println("Controller disconnected.");
	}

	public static void main(String[] args) throws Exception {

		FederationController controller = new FederationController();
		controller.start();

	}
}