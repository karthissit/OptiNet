package com.optinet.simulator.api;

import com.optinet.simulator.topology.NetworkTopology;
import com.optinet.simulator.topology.SimulatedNetworkElement;
import com.optinet.simulator.topology.Port;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Management REST API for the Network Element Simulator
 * 
 * This API allows external management of:
 * - Network topology (add/remove NEs)
 * - Port connections
 * - Topology queries
 * 
 * Note: This is separate from NETCONF protocol.
 * NEs respond to NETCONF RPCs via SSH on port 8830.
 * This API is on standard HTTP port 8080 for management.
 * 
 * Example usage:
 *   GET  /api/simulator/topology
 *   GET  /api/simulator/ne/{neId}
 *   POST /api/simulator/connect (to connect ports)
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorManagementController {
    
    private final NetworkTopology topology;
    
    public SimulatorManagementController(NetworkTopology topology) {
        this.topology = topology;
    }
    
    /**
     * Get full network topology
     */
    @GetMapping("/topology")
    public Map<String, Object> getTopology() {
        List<SimulatedNetworkElement> nes = topology.getAllNetworkElements();
        List<NetworkTopology.Connection> connections = topology.getAllConnections();
        
        return Map.of(
            "networkElementCount", nes.size(),
            "connectionCount", connections.size(),
            "networkElements", nes.stream()
                .map(ne -> Map.of(
                    "neId", ne.getNeId(),
                    "neType", ne.getNeType(),
                    "location", ne.getLocation(),
                    "portCount", ne.getAllPorts().size(),
                    "connectedPorts", ne.getConnectedPorts().size()
                ))
                .toList(),
            "connections", connections.stream()
                .map(c -> Map.of(
                    "from", c.fromNeId() + ":" + c.fromPortId(),
                    "to", c.toNeId() + ":" + c.toPortId(),
                    "degradationDb", c.signalDegradationDb()
                ))
                .toList()
        );
    }
    
    /**
     * Get details of a specific network element
     */
    @GetMapping("/ne/{neId}")
    public Map<String, Object> getNetworkElement(@PathVariable String neId) {
        SimulatedNetworkElement ne = topology.getNetworkElement(neId);
        if (ne == null) {
            throw new RuntimeException("Network element not found: " + neId);
        }
        
        return Map.of(
            "neId", ne.getNeId(),
            "neType", ne.getNeType(),
            "location", ne.getLocation(),
            "lastActivity", ne.getLastActivity(),
            "ports", ne.getAllPorts().stream()
                .map(p -> Map.of(
                    "portId", p.portId(),
                    "portName", p.portName(),
                    "portType", p.portType().toString(),
                    "connectionStatus", p.connectionStatus(),
                    "connectedNeId", p.connectedNeId(),
                    "connectedPortId", p.connectedPortId(),
                    "transmitPower", p.transmitPower(),
                    "receivePower", p.receivePower(),
                    "wavelength", p.wavelength()
                ))
                .toList()
        );
    }
    
    /**
     * Connect two NE ports
     */
    @PostMapping("/connect")
    public Map<String, Object> connectPorts(
        @RequestParam String fromNeId,
        @RequestParam String fromPortId,
        @RequestParam String toNeId,
        @RequestParam String toPortId,
        @RequestParam(defaultValue = "0.0") double degradationDb) {
        
        boolean success = topology.connect(fromNeId, fromPortId, toNeId, toPortId, degradationDb);
        
        return Map.of(
            "success", success,
            "message", success ? "Connection established" : "Connection failed"
        );
    }
    
    /**
     * Disconnect a port
     */
    @PostMapping("/disconnect")
    public Map<String, Object> disconnectPort(
        @RequestParam String neId,
        @RequestParam String portId) {
        
        boolean success = topology.disconnect(neId, portId);
        
        return Map.of(
            "success", success,
            "message", success ? "Port disconnected" : "Disconnection failed"
        );
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "networkElements", topology.getNetworkElementCount(),
            "connections", topology.getAllConnections().size(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
