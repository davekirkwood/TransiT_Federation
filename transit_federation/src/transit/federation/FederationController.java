package transit.federation;

import java.net.URL;
import java.util.Scanner;

import hla.rti1516e.CallbackModel;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import transit.federation.federate.TransitFederate;

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
			System.out.println("Federation created: " + FEDERATION_NAME);

		} catch (FederationExecutionAlreadyExists e) {
			System.out.println("Federation already exists.");
		}

		rtiamb.disconnect();
		rtiamb = null;

		// Keep connection open - federates will connect later
		System.out.println("Portico RTI is running.");
		System.out.println("Federation: " + FEDERATION_NAME);
		System.out.println("Waiting for federates to connect...");
	}

	public void stop() throws Exception {

		System.out.println("Stopping federation...");

		// Now all federates have resigned, safely destroy the federation
		try {
			rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
			rtiamb.connect(new NullFederateAmbassador(), CallbackModel.HLA_IMMEDIATE);
			rtiamb.destroyFederationExecution(FEDERATION_NAME);
			System.out.println("Federation destroyed.");
		} catch (FederatesCurrentlyJoined e) {
			System.out.println("Cannot destroy federation: federates are still joined.");
		}

		rtiamb.disconnect();

		System.out.println("Controller disconnected.");
	}

	public static void main(String[] args) {

		FederationController controller = new FederationController();

		try {
			controller.start();

			// Keep the controller alive
			System.out.println("Press ENTER to stop.");
			new Scanner(System.in).nextLine();

			controller.stop();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}