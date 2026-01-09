package com.optinet.simulator.topology;

/**
 * Builds predefined network topologies for simulation.
 * 
 * Simulates real-world optical network scenarios:
 * 1. Simple Point-to-Point: Transponder → Amplifier → Transponder
 * 2. Long-Haul: Multi-span path with multiple amplifiers
 * 3. Mesh Network: Multiple paths with cross-connects
 * 4. Ring Network: Redundant ring topology for protection
 * 
 * Each scenario includes realistic signal degradation values.
 */
public class TopologyBuilder {
    
    /**
     * Build a simple point-to-point connection
     * 
     * [TXP-A] ---fiber--- [AMP-1] ---fiber--- [TXP-B]
     *   |                    |                    |
     *   NY-DC              Repeater              BOS-DC
     */
    public static NetworkTopology buildPointToPointTopology() {
        NetworkTopology topo = new NetworkTopology();
        
        // Add network elements
        topo.addNetworkElement("OTN-TXP-NYC-001", "TRANSPONDER", "New York DataCenter");
        topo.addNetworkElement("EDFA-REPEAT-001", "AMPLIFIER", "Repeater Station");
        topo.addNetworkElement("OTN-TXP-BOS-001", "TRANSPONDER", "Boston DataCenter");
        
        // Connect: TXP-NYC optical-tx → Amplifier input-1
        // Fiber loss: 80km * 0.2 dB/km = 16 dB degradation
        topo.connect("OTN-TXP-NYC-001", "optical-tx", 
                     "EDFA-REPEAT-001", "input-1", 16.0);
        
        // Connect: Amplifier output-1 → TXP-BOS optical-rx
        // Fiber loss: 80km * 0.2 dB/km = 16 dB degradation
        topo.connect("EDFA-REPEAT-001", "output-1", 
                     "OTN-TXP-BOS-001", "optical-rx", 16.0);
        
        return topo;
    }
    
    /**
     * Build a long-haul network with multiple spans
     * 
     * [TXP-NYC] ---80km--- [AMP-1] ---80km--- [AMP-2] ---80km--- [TXP-BOS]
     * 
     * Each span: 16 dB degradation (80 km * 0.2 dB/km)
     * Amplifier gain: 25 dB (recovers from span loss and adds margin)
     */
    public static NetworkTopology buildLongHaulTopology() {
        NetworkTopology topo = new NetworkTopology();
        
        // Endpoints
        topo.addNetworkElement("OTN-TXP-NYC-001", "TRANSPONDER", "New York DataCenter");
        topo.addNetworkElement("OTN-TXP-BOS-001", "TRANSPONDER", "Boston DataCenter");
        
        // Amplifier stages
        topo.addNetworkElement("EDFA-NYC-001", "AMPLIFIER", "New York Span 1");
        topo.addNetworkElement("EDFA-PHILLY-001", "AMPLIFIER", "Philadelphia Span 2");
        topo.addNetworkElement("EDFA-BOS-001", "AMPLIFIER", "Boston Span 3");
        
        // Span 1: NYC → Philadelphia (80 km)
        topo.connect("OTN-TXP-NYC-001", "optical-tx", 
                     "EDFA-NYC-001", "input-1", 16.0);
        topo.connect("EDFA-NYC-001", "output-1", 
                     "EDFA-PHILLY-001", "input-1", 16.0);
        
        // Span 2: Philadelphia → Boston (80 km)
        topo.connect("EDFA-PHILLY-001", "output-1", 
                     "EDFA-BOS-001", "input-1", 16.0);
        
        // Span 3: Boston → Receiver (80 km)
        topo.connect("EDFA-BOS-001", "output-1", 
                     "OTN-TXP-BOS-001", "optical-rx", 16.0);
        
        return topo;
    }
    
    /**
     * Build a mesh network with cross-connects
     * 
     *        [TXP-NYC]
     *         /  |  \
     *        /   |   \
     *     [OXC1] | [OXC2]
     *        \   |   /
     *         \  |  /
     *        [TXP-BOS]
     * 
     * Multiple paths enable service routing and protection.
     */
    public static NetworkTopology buildMeshTopology() {
        NetworkTopology topo = new NetworkTopology();
        
        // Endpoints
        topo.addNetworkElement("OTN-TXP-NYC-001", "TRANSPONDER", "New York");
        topo.addNetworkElement("OTN-TXP-BOS-001", "TRANSPONDER", "Boston");
        
        // Cross-connects (wavelength routers)
        topo.addNetworkElement("OXC-PHILLY-001", "CROSS-CONNECT", "Philadelphia");
        topo.addNetworkElement("OXC-DC-001", "CROSS-CONNECT", "DC");
        
        // Amplifiers for long spans
        topo.addNetworkElement("EDFA-NYC-001", "AMPLIFIER", "NYC");
        topo.addNetworkElement("EDFA-BOS-001", "AMPLIFIER", "BOS");
        
        // Primary path: NYC → OXC-PHILLY → BOS
        topo.connect("OTN-TXP-NYC-001", "optical-tx", 
                     "EDFA-NYC-001", "input-1", 8.0);
        topo.connect("EDFA-NYC-001", "output-1", 
                     "OXC-PHILLY-001", "wavelength-1", 8.0);
        topo.connect("OXC-PHILLY-001", "wavelength-2", 
                     "EDFA-BOS-001", "input-1", 8.0);
        topo.connect("EDFA-BOS-001", "output-1", 
                     "OTN-TXP-BOS-001", "optical-rx", 8.0);
        
        // Alternate path: NYC → OXC-DC → BOS
        topo.connect("OTN-TXP-NYC-001", "optical-tx", 
                     "OXC-DC-001", "wavelength-1", 12.0);
        topo.connect("OXC-DC-001", "wavelength-2", 
                     "OTN-TXP-BOS-001", "optical-rx", 12.0);
        
        return topo;
    }
    
    /**
     * Build a ring topology (protection ring)
     * 
     * Ring topologies provide automatic protection switching (APS):
     * - Primary path: clockwise around ring
     * - Backup path: counter-clockwise around ring
     * - If primary fails, traffic switches to backup within milliseconds
     * 
     *        [NYC]
     *        / | \
     *    [P1] | [P2]
     *      |  |  |
     *    [BOS] [DC]
     */
    public static NetworkTopology buildRingTopology() {
        NetworkTopology topo = new NetworkTopology();
        
        // Ring nodes (all transponders in this example)
        topo.addNetworkElement("OTN-TXP-NYC-001", "TRANSPONDER", "New York");
        topo.addNetworkElement("OTN-TXP-PHILLY-001", "TRANSPONDER", "Philadelphia");
        topo.addNetworkElement("OTN-TXP-DC-001", "TRANSPONDER", "Washington DC");
        topo.addNetworkElement("OTN-TXP-BOS-001", "TRANSPONDER", "Boston");
        
        // Amplifiers for inter-node spans
        topo.addNetworkElement("EDFA-SPAN-1", "AMPLIFIER", "NYC-PHILLY");
        topo.addNetworkElement("EDFA-SPAN-2", "AMPLIFIER", "PHILLY-DC");
        topo.addNetworkElement("EDFA-SPAN-3", "AMPLIFIER", "DC-BOS");
        topo.addNetworkElement("EDFA-SPAN-4", "AMPLIFIER", "BOS-NYC");
        
        // Ring connections (clockwise)
        // NYC → Philly
        topo.connect("OTN-TXP-NYC-001", "optical-tx", 
                     "EDFA-SPAN-1", "input-1", 10.0);
        topo.connect("EDFA-SPAN-1", "output-1", 
                     "OTN-TXP-PHILLY-001", "optical-rx", 10.0);
        
        // Philly → DC
        topo.connect("OTN-TXP-PHILLY-001", "optical-tx", 
                     "EDFA-SPAN-2", "input-1", 10.0);
        topo.connect("EDFA-SPAN-2", "output-1", 
                     "OTN-TXP-DC-001", "optical-rx", 10.0);
        
        // DC → Boston
        topo.connect("OTN-TXP-DC-001", "optical-tx", 
                     "EDFA-SPAN-3", "input-1", 10.0);
        topo.connect("EDFA-SPAN-3", "output-1", 
                     "OTN-TXP-BOS-001", "optical-rx", 10.0);
        
        // Boston → NYC (completes ring)
        topo.connect("OTN-TXP-BOS-001", "optical-tx", 
                     "EDFA-SPAN-4", "input-1", 10.0);
        topo.connect("EDFA-SPAN-4", "output-1", 
                     "OTN-TXP-NYC-001", "optical-rx", 10.0);
        
        return topo;
    }
    
    /**
     * Create a complex realistic backbone network
     * 
     * Combines multiple topologies: long-haul + mesh + ring
     * Represents a real optical backbone with:
     * - Long-haul transcontinental paths
     * - Regional OXC points
     * - Protected ring services
     */
    public static NetworkTopology buildComplexBackboneTopology() {
        // Start with point-to-point
        NetworkTopology topo = buildPointToPointTopology();
        
        // Add regional cross-connects
        topo.addNetworkElement("OXC-NYC-001", "CROSS-CONNECT", "New York");
        topo.addNetworkElement("OXC-BOS-001", "CROSS-CONNECT", "Boston");
        
        // Add additional transponders for services
        topo.addNetworkElement("OTN-TXP-NYC-002", "TRANSPONDER", "New York");
        topo.addNetworkElement("OTN-TXP-NYC-003", "TRANSPONDER", "New York");
        
        return topo;
    }
}
