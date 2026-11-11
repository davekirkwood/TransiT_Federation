package transit.federation.federate;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;

public class TransitFederateAmbassador extends NullFederateAmbassador {

	private final TransitFederate federate;

	public TransitFederateAmbassador(TransitFederate federate) {
		this.federate = federate;
	}

	@Override
	public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap parameters, byte[] tag, OrderType sentOrdering, TransportationTypeHandle transport,
			SupplementalReceiveInfo receiveInfo) {

		federate.receiveInteraction(interactionClass, parameters, tag, sentOrdering, transport, receiveInfo);
	}

	@Override
	public void connectionLost(String faultDescription) {
		System.err.println("RTI connection lost: " + faultDescription);
	}
}