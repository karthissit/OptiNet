package com.optinet.simulator.topology;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network Topology Manager
 * 
 * Manages the complete network topology:
 * - Maintains all simulated network elements
 * - Manages port connections between NEs
 * - Tracks network paths and signal quality
 * 
 * This represents a real optical network:
 * ```
 * [Transponder] --(optical fiber)-- [Amplifier] --(fiber)-- [OXC] --(fiber)-- [Amplifier] -- [Transponder]
 * ```
 * 
 * Each connection can have simulated signal degradation due to:
 * - Fiber loss (~0.2 dB/km)
 * - Amplifier noise
 * - Chromatic dispersion
 * - Polarization mode dispersion
 */
public class NetworkTopology {
    private final Map<String, SimulatedNetworkElement> networkElements;
    private final List<Connection> connections;
    
    public NetworkTopology() {
        this.networkElements = new ConcurrentHashMap<>();
        this.connections = Collections.synchronizedList(new ArrayList<>());
    }
    
    /**
     * Add a network element to the topology
     */
    public void addNetworkElement(String neId, String neType, String location) {
        networkElements.put(neId, new SimulatedNetworkElement(neId, neType, location));
    }
    
    /**
     * Connect two NEs by their ports
     */
    public boolean connect(String fromNeId, String fromPortId, 
                          String toNeId, String toPortId, 
                          double signalDegradationDb) {
        SimulatedNetworkElement fromNe = networkElements.get(fromNeId);
        SimulatedNetworkElement toNe = networkElements.get(toNeId);
        
        if (fromNe == null || toNe == null) {
            return false;
        }
        
        boolean connected = fromNe.connectPort(fromPortId, toNe, toPortId, signalDegradationDb);
        if (connected) {
            connections.add(new Connection(fromNeId, fromPortId, toNeId, toPortId, signalDegradationDb));
        }
        
        return connected;
    }
    
    /**
     * Disconnect two NEs
     */
    public boolean disconnect(String neId, String portId) {
        SimulatedNetworkElement ne = networkElements.get(neId);
        if (ne == null) {
            return false;
        }
        
        boolean disconnected = ne.disconnectPort(portId);
        if (disconnected) {
            connections.removeIf(c -> 
                (c.fromNeId.equals(neId) && c.fromPortId.equals(portId)) ||
                (c.toNeId.equals(neId) && c.toPortId.equals(portId))
            );
        }
        
        return disconnected;
    }
    
    public SimulatedNetworkElement getNetworkElement(String neId) {
        return networkElements.get(neId);
    }
    
    public List<SimulatedNetworkElement> getAllNetworkElements() {
        return new ArrayList<>(networkElements.values());
    }
    
    public List<Connection> getAllConnections() {
        return new ArrayList<>(connections);
    }
    
    public int getNetworkElementCount() {
        return networkElements.size();
    }
    
    /**
     * Represents a connection (link) between two NEs
     */
    public record Connection(
        String fromNeId,
        String fromPortId,
        String toNeId,
        String toPortId,
        double signalDegradationDb
    ) {}
}
