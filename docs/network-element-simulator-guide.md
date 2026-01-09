# Network Element Simulator Design & User Guide

## Overview

The Network Element Simulator is a Spring Boot module that simulates optical network elements (Transponders, Amplifiers, Switches, Cross-Connects) with realistic NETCONF protocol support over SSH.

**Purpose:**
- Learn NETCONF protocol implementation
- Simulate real optical network topologies
- Test network management systems (like the OptiNet API)
- Develop and debug without real hardware

---

## Architecture

### Module Structure

```
network-element-simulator/
├── src/main/java/com/optinet/simulator/
│   ├── NeSimulatorApplication.java       # Spring Boot entry point
│   │
│   ├── netconf/
│   │   ├── NetconfRpc.java              # RPC message handling
│   │   ├── NetconfSession.java          # Client session management
│   │   └── RpcHandler.java              # RPC operation processing
│   │
│   ├── topology/
│   │   ├── NetworkTopology.java         # Topology graph manager
│   │   ├── SimulatedNetworkElement.java # NE with ports
│   │   ├── Port.java                    # Port abstraction
│   │   ├── PortType.java                # Optical, Electrical, etc.
│   │   └── TopologyBuilder.java         # Predefined topologies
│   │
│   ├── auth/
│   │   └── AuthenticationManager.java   # SSH authentication
│   │
│   ├── config/
│   │   └── SimulatorConfiguration.java  # Spring config
│   │
│   └── api/
│       └── SimulatorManagementController.java  # Management REST API
│
└── src/main/resources/
    └── application.yml                  # Configuration
```

### Key Components

#### 1. Network Topology
Manages the complete network:
```
NetworkTopology
├── NetworkElement-1 (Transponder)
│   └── Ports: eth1, eth2, optical-tx, optical-rx
├── NetworkElement-2 (Amplifier)
│   └── Ports: input-1, output-1, input-2, output-2
├── NetworkElement-3 (OXC)
│   └── Ports: wavelength-1, wavelength-2, ...
└── Connections: link descriptions with signal degradation
```

#### 2. Simulated Network Elements
Each NE simulates:
- **Ports:** Optical, electrical, control
- **Port connections:** To other NEs
- **Signal degradation:** Fiber loss, amplifier gain
- **RPC handling:** GET, GET-CONFIG, EDIT-CONFIG

#### 3. NETCONF Server
- SSH listener on port 8830
- Receives RPC messages
- Returns XML responses
- Handles session lifecycle

#### 4. REST Management API
HTTP API on port 8080:
- Query network topology
- Connect/disconnect ports
- Manage simulated elements

---

## Network Topologies

### 1. Point-to-Point
```
[Transponder-NYC] --fiber-- [Amplifier] --fiber-- [Transponder-BOS]
```
- Simple linear path
- Good for learning basics
- 2 transponders, 1 amplifier

### 2. Long-Haul
```
[TXP] --80km-- [AMP] --80km-- [AMP] --80km-- [TXP]
 NYC    Span1  PHILLY Span2    BOS   Span3  Boston
```
- Multi-span long-distance path
- Realistic fiber loss (0.2 dB/km)
- Amplifier regeneration
- 2 transponders, 3 amplifiers

### 3. Mesh Network
```
       [TXP-NYC]
        /  |  \
    [OXC] | [OXC]
       \   |   /
      [TXP-BOS]
```
- Multiple paths (primary + backup)
- Wavelength routing
- Protection switching capable
- 2 transponders, 2 OXCs

### 4. Ring Network
```
      [TXP-NYC]
      /        \
  [AMP]      [AMP]
   /             \
[TXP-PHILLY]  [TXP-BOS]
    |              |
  [AMP]          [AMP]
     \            /
    [TXP-DC]-----
```
- Ring protection (APS)
- Automatic failover
- Redundancy
- 4 transponders, 4 amplifiers

---

## NETCONF Protocol Details

### SSH Connection

```bash
# Connect to network element
ssh -p 8830 admin@localhost
# Password: admin

# You're now in NETCONF session
# Send RPC messages (XML)
```

### Message Format

**RPC Request:**
```xml
<rpc message-id="1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
  <get/>
</rpc>
]]>]]>
```

Note: Message must end with `]]>]]>` delimiter

**RPC-REPLY:**
```xml
<rpc-reply message-id="1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
  <data>
    <system>
      <hostname>OTN-TXP-NYC-001</hostname>
      <ne-type>TRANSPONDER</ne-type>
      <status>operational</status>
      <interfaces>
        <interface>
          <name>eth1</name>
          <admin-status>up</admin-status>
          <oper-status>up</oper-status>
        </interface>
      </interfaces>
    </system>
  </data>
</rpc-reply>
]]>]]>
```

### Supported Operations

#### 1. GET (retrieve operational data)
```xml
<rpc message-id="1">
  <get/>
</rpc>
]]>]]>
```

Response: Complete system state (interfaces, status, etc.)

#### 2. GET-CONFIG (retrieve configuration)
```xml
<rpc message-id="2">
  <get-config>
    <source>
      <running/>
    </source>
  </get-config>
</rpc>
]]>]]>
```

Response: Configuration parameters

#### 3. EDIT-CONFIG (modify configuration)
```xml
<rpc message-id="3">
  <edit-config>
    <target>
      <running/>
    </target>
    <config>
      <hostname>OTN-TXP-NYC-NEW</hostname>
    </config>
  </edit-config>
</rpc>
]]>]]>
```

Response: `<ok/>` if successful

---

## REST Management API

### Base URL
```
http://localhost:8080/api/simulator
```

### Endpoints

#### 1. GET /topology
Get complete network topology

**Response:**
```json
{
  "networkElementCount": 7,
  "connectionCount": 6,
  "networkElements": [
    {
      "neId": "OTN-TXP-NYC-001",
      "neType": "TRANSPONDER",
      "location": "New York DataCenter",
      "portCount": 4,
      "connectedPorts": 1
    }
  ],
  "connections": [
    {
      "from": "OTN-TXP-NYC-001:optical-tx",
      "to": "EDFA-NYC-001:input-1",
      "degradationDb": 16.0
    }
  ]
}
```

#### 2. GET /ne/{neId}
Get details of a specific NE

**Example:**
```bash
curl http://localhost:8080/api/simulator/ne/OTN-TXP-NYC-001
```

**Response:**
```json
{
  "neId": "OTN-TXP-NYC-001",
  "neType": "TRANSPONDER",
  "location": "New York DataCenter",
  "lastActivity": 1705318245000,
  "ports": [
    {
      "portId": "eth1",
      "portName": "Client Interface 1",
      "portType": "ELECTRICAL",
      "connectionStatus": "AVAILABLE",
      "connectedNeId": null,
      "transmitPower": 0.0,
      "receivePower": 0.0
    },
    {
      "portId": "optical-tx",
      "portName": "Line TX",
      "portType": "OPTICAL",
      "connectionStatus": "CONNECTED",
      "connectedNeId": "EDFA-NYC-001",
      "connectedPortId": "input-1",
      "transmitPower": -8.0,
      "receivePower": -24.0,
      "wavelength": "1550.12nm"
    }
  ]
}
```

#### 3. POST /connect
Connect two NE ports

**Parameters:**
- `fromNeId`: Source NE ID
- `fromPortId`: Source port ID
- `toNeId`: Destination NE ID
- `toPortId`: Destination port ID
- `degradationDb`: Signal loss in dB (default: 0)

**Example:**
```bash
curl -X POST "http://localhost:8080/api/simulator/connect?fromNeId=OTN-TXP-NYC-001&fromPortId=eth1&toNeId=OTN-TXP-BOS-001&toPortId=eth1&degradationDb=5.0"
```

#### 4. POST /disconnect
Disconnect a port

**Parameters:**
- `neId`: Network element ID
- `portId`: Port ID

#### 5. GET /health
Health check

---

## Running the Simulator

### Prerequisite
- Java 17+
- Gradle 7.0+

### Build
```bash
cd /workspaces/OptiNet
gradle clean build -p network-element-simulator
```

### Run with Gradle
```bash
gradle bootRun -p network-element-simulator
```

Output:
```
...
2024-01-15 10:30:45 INFO  ... Started NeSimulatorApplication in 3.5 seconds
2024-01-15 10:30:45 INFO  ... Initialized NetworkTopology with 7 NEs and 6 connections
2024-01-15 10:30:45 INFO  ... NETCONF server listening on 0.0.0.0:8830
```

### Run with JAR
```bash
java -jar network-element-simulator/build/libs/network-element-simulator.jar
```

### Check if Running
```bash
# HTTP API health check
curl http://localhost:8080/api/simulator/health

# NETCONF SSH (requires SSH client)
ssh -p 8830 admin@localhost
```

---

## Testing the Simulator

### 1. Test REST API

```bash
# Get topology
curl http://localhost:8080/api/simulator/topology | jq

# Get specific NE
curl http://localhost:8080/api/simulator/ne/OTN-TXP-NYC-001 | jq

# Connect ports
curl -X POST "http://localhost:8080/api/simulator/connect?fromNeId=OTN-TXP-NYC-001&fromPortId=eth1&toNeId=OTN-TXP-BOS-001&toPortId=eth1&degradationDb=5.0"

# Get topology again (should show new connection)
curl http://localhost:8080/api/simulator/topology | jq
```

### 2. Test NETCONF (SSH)

Create a file `test.xml`:
```xml
<rpc message-id="1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
  <get/>
</rpc>
]]>]]>
```

Send via SSH:
```bash
(cat test.xml; sleep 1) | ssh -p 8830 admin@localhost
```

### 3. Test with netcat

```bash
# Connect to SSH port
nc -v localhost 8830

# (NETCONF uses SSH subsystem; netcat won't work)
# Use SSH client instead
```

---

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
simulator:
  netconf:
    ssh-port: 8830          # Change SSH port
    max-sessions: 10        # Max concurrent NETCONF sessions
    session-timeout-ms: 300000  # 5 minutes
  
  topology:
    default-topology: LONG_HAUL  # POINT_TO_POINT, LONG_HAUL, MESH, RING, COMPLEX
  
  simulation:
    kpi-generation-enabled: true  # Generate fake KPI data
    kpi-interval-ms: 5000        # Every 5 seconds
    add-noise: true
    noise-level: 0.5
```

---

## Integration with OptiNet API

The simulator is designed to work alongside the OptiNet API (Phase 1):

1. **Simulator** (Port 8830): Simulates network elements via NETCONF/SSH
2. **OptiNet API** (Port 8080): Management system that queries NEs
3. **PostgreSQL**: Stores telemetry, alarms, SLA records

### Future Integration
- OptiNet API will send NETCONF queries to simulator NEs
- Parse responses and store in database
- Simulate real network management workflow

---

## Extending the Simulator

### Adding a New Network Element Type

Edit `SimulatedNetworkElement.java`:

```java
private void initializeMyNewType() {
    // Add custom ports
    ports.put("custom-1", Port.createOpticalPort("custom-1", "Custom Port", -5.0, "1550nm"));
    // ... more ports
}
```

Update switch in `initializePorts()`:
```java
case "my-new-type":
    initializeMyNewType();
    break;
```

### Adding a New Topology

Edit `TopologyBuilder.java`:

```java
public static NetworkTopology buildMyTopology() {
    NetworkTopology topo = new NetworkTopology();
    topo.addNetworkElement("NE-1", "TRANSPONDER", "Location-1");
    // ... more elements and connections
    return topo;
}
```

---

## Troubleshooting

### SSH Connection Refused
- Check port 8830 is open: `netstat -an | grep 8830`
- Check application is running: `curl http://localhost:8080/api/simulator/health`
- Try restarting with `gradle bootRun`

### NETCONF Handshake Fails
- Ensure message ends with `]]>]]>`
- Check XML syntax (well-formed)
- Verify NETCONF namespace

### API Returns 404
- Ensure simulator is running
- Check base URL: `http://localhost:8080/api/simulator`
- Verify NE ID exists in topology

---

## References

- [NETCONF RFC 6241](https://tools.ietf.org/html/rfc6241)
- [NETCONF 1.0 Operations](https://tools.ietf.org/html/rfc6241#section-7)
- [Optical Network Basics](#domain-model)

---

**Next Steps:**
1. Run the simulator locally
2. Query the REST API to understand topology
3. Build the OptiNet API module (Phase 1)
4. Integrate OptiNet API with simulator via NETCONF
