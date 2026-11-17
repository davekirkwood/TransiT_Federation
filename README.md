# Transit Federation

Transit Federation is a Java-based simulation federation framework developed as part of the **TransiT (Digital Twinning for Transport Decarbonisation)** research programme, specifically **Challenge-Led Demonstrator 2 (CLD2) – Passenger Transport**.

The project uses **Portico**, an implementation of the High Level Architecture (HLA) standard, to connect and synchronise multiple transport simulators.

The initial federation supports three simulators:

- **BRaVE** – Birmingham Railway Virtual Environment
- **SUMO** – Simulation of Urban MObility
- **MATSim** – Multi-Agent Transport Simulation

The long-term objective is to develop a generic, extensible framework capable of integrating additional simulators. Each simulator should be optional, with one participating simulator designated as the master responsible for controlling simulation time.

## TransiT Project

This software is being developed as part of the [TransiT research programme](https://transit.ac.uk/), a UK research initiative focused on accelerating transport decarbonisation through digital twinning.

TransiT aims to develop interconnected digital twins spanning different transport modes, enabling researchers and transport planners to investigate how changes to transport systems, infrastructure and passenger behaviour can contribute to reducing carbon emissions.

### Challenge-Led Demonstrator 2 (CLD2)

Transit Federation contributes to **CLD2 – Passenger Transport**, which focuses on developing an integrated passenger transport digital twin for the West Midlands.

CLD2 brings together simulation approaches covering:

- Rail transport
- Road traffic
- Public transport, including buses and trams
- Active travel, including walking and cycling
- Multimodal passenger journeys

The demonstrator aims to support the investigation of transport interventions, passenger behaviour and decarbonisation strategies across interconnected transport networks.

### Contribution of Transit Federation

Transit Federation is intended to provide a common simulation federation infrastructure through which different transport simulation platforms can operate together.

Its objectives include:

- Enabling interoperability between BRaVE, SUMO and MATSim.
- Supporting synchronised simulation time across participating simulators.
- Allowing different combinations of simulators to be used.
- Providing a configurable master simulation responsible for time progression.
- Establishing a generic architecture that can accommodate additional simulators.

The current implementation is a prototype in which the federate classes emulate their corresponding simulators. Direct integration with the actual simulation engines is planned for future development.

### Further Information

- [TransiT – Official Website](https://transit.ac.uk/)
- [TransiT – Challenge-Led Demonstrators](https://transit.ac.uk/our-demonstrators/)
- [CLD2 – Passenger Transport Demonstrator](https://transit.ac.uk/our-demonstrators/demonstrator-2-passenger-transport/)

## Federation Architecture

The federation currently consists of four main Java classes:

| Class | Role |
|---|---|
| `FederationController` | Initialises and coordinates the federation |
| `BRaVEFederate` | Prototype federate representing BRaVE |
| `SUMOFederate` | Prototype federate representing SUMO |
| `MATSimFederate` | Prototype federate representing MATSim |

The federates communicate through the Portico HLA Run-Time Infrastructure.

### Simulation Time Management

The intended architecture requires one participating simulator to act as the master simulation and control the progression of simulation time.

All other participating simulators should remain synchronised with the master.

The master should be configurable, and no particular simulator should be mandatory.

## Current Development Status

**Status: Prototype / Early Development**

The current federate implementations simulate the behaviour of their respective simulators rather than connecting directly to the actual simulation engines.

### Planned Development

- [x] Establish Maven-based Java project
- [x] Configure Java 21
- [x] Create initial Portico federation
- [x] Create BRaVE, SUMO and MATSim federate classes
- [ ] Integrate with the actual BRaVE simulator
- [ ] Integrate with the actual SUMO simulator
- [ ] Integrate with the actual MATSim simulator
- [ ] Support optional simulator participation
- [ ] Implement configurable master simulation selection
- [ ] Develop a generic simulator integration interface

## Requirements

- Java 21
- Apache Maven
- Portico HLA RTI
- Eclipse IDE (recommended)

## Building the Project

Build the project using Maven:

```bash
mvn clean install
```

## Running the Federation

The current prototype should be launched in the following order:

1. `FederationController`
2. `BRaVEFederate`
3. `SUMOFederate`
4. `MATSimFederate`

### Running in Eclipse

For each class:

1. Locate the class in the Eclipse Package Explorer.
2. Right-click the class.
3. Select **Run As → Java Application**.
4. Start the next class in the specified order.

The current prototype uses all three federates. Future versions are intended to support different combinations of simulators.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core implementation |
| Maven | Build and dependency management |
| Portico | HLA Run-Time Infrastructure |
| HLA | Federation communication and time coordination |
| BRaVE | Railway simulation |
| SUMO | Road traffic simulation |
| MATSim | Multi-agent transport simulation |

## License

License information to be added.
