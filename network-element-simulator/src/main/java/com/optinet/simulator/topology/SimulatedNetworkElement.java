package com.optinet.simulator.topology;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a simulated network element with ports and connections.
 * 
 * A network element can be:
 * - Transponder: Has client (electrical) and line (optical) ports
 * - Amplifier: Has input/output optical ports
 * - Switch/OXC: Has multiple optical ports for routing
 * 
 * Each NE maintains its own port state and can be queried via NETCONF.
 */
public class SimulatedNetworkElement {
    private final String neId;
    private final String neType;
    private final String location;
    private final Map<String, Port> ports;
    private volatile long lastActivity;
    
    public SimulatedNetworkElement(String neId, String neType, String location) {
        this.neId = neId;
        this.neType = neType;
        this.location = location;
        this.ports = new ConcurrentHashMap<>();
        this.lastActivity = System.currentTimeMillis();
        initializePorts();
    }
    
    /**
     * Initialize ports based on NE type
     */
    private void initializePorts() {
        switch (neType.toLowerCase()) {
            case "transponder":
                initializeTransponderPorts();
                break;
            case "amplifier":
                initializeAmplifierPorts();
                break;
            case "switch":
                initializeSwitchPorts();
                break;
            case "cross-connect":
                initializeCrossConnectPorts();
                break;
            default:
                initializeGenericPorts();
        }
    }
    
    private void initializeTransponderPorts() {
        // Client (electrical) ports: typically 1-10 per transponder
        ports.put("eth1", Port.createElectricalPort("eth1", "Client Interface 1"));
        ports.put("eth2", Port.createElectricalPort("eth2", "Client Interface 2"));
        
        // Optical (line) ports: TX and RX at different wavelengths
        ports.put("optical-tx", 
            Port.createOpticalPort("optical-tx", "Line TX", -8.0, "1550.12nm"));
        ports.put("optical-rx", 
            Port.createOpticalPort("optical-rx", "Line RX", 0.0, "1550.12nm"));
    }
    
    private void initializeAmplifierPorts() {
        // EDFA (Erbium-Doped Fiber Amplifier) typically has:
        // - Input optical ports (from previous span)
        // - Output optical ports (to next span)
        ports.put("input-1", 
            Port.createOpticalPort("input-1", "Input Port 1", -20.0, "C-Band"));
        ports.put("output-1", 
            Port.createOpticalPort("output-1", "Output Port 1", 17.0, "C-Band"));
        
        // Secondary amplifier stage
        ports.put("input-2", 
            Port.createOpticalPort("input-2", "Input Port 2", -18.0, "C-Band"));
        ports.put("output-2", 
            Port.createOpticalPort("output-2", "Output Port 2", 18.0, "C-Band"));
    }
    
    private void initializeSwitchPorts() {
        // Optical switches have many ports for routing
        for (int i = 1; i <= 8; i++) {
            String portId = "port-" + i;
            String portName = "Optical Port " + i;
            ports.put(portId, Port.createOpticalPort(portId, portName, -5.0, "Variable"));
        }
    }
    
    private void initializeCrossConnectPorts() {
        // OXC has wavelength-specific ports
        for (int i = 1; i <= 4; i++) {
            String portId = "wavelength-" + i;
            String portName = "Wavelength Channel " + i;
            ports.put(portId, Port.createOpticalPort(portId, portName, 0.0, String.format("155%d.nm", 50 + i)));
        }
    }
    
    private void initializeGenericPorts() {
        // Generic: 4 electrical + 4 optical
        for (int i = 1; i <= 4; i++) {
            ports.put("eth" + i, Port.createElectricalPort("eth" + i, "Ethernet " + i));
            ports.put("optical-" + i, Port.createOpticalPort("optical-" + i, "Optical " + i, -8.0, "Variable"));
        }
    }
    
    /**
     * Connect this NE's port to another NE's port
     */
    public boolean connectPort(String localPortId, SimulatedNetworkElement remoteNe, 
                              String remotePortId, double signalDegradation) {
        Port localPort = ports.get(localPortId);
        Port remotePort = remoteNe.ports.get(remotePortId);
        
        if (localPort == null || remotePort == null) {
            return false;
        }
        
        // Calculate RX power considering signal degradation
        double rxPower = localPort.transmitPower() - signalDegradation;
        
        // Update both sides of connection
        ports.put(localPortId, localPort.withConnection(remoteNe.neId, remotePortId, rxPower));
        remoteNe.ports.put(remotePortId, remotePort.withConnection(this.neId, localPortId, rxPower - 0.5));
        
        this.lastActivity = System.currentTimeMillis();
        remoteNe.lastActivity = System.currentTimeMillis();
        
        return true;
    }
    
    /**
     * Disconnect a port
     */
    public boolean disconnectPort(String portId) {
        Port port = ports.get(portId);
        if (port == null || !port.isConnected()) {
            return false;
        }
        
        ports.put(portId, port.disconnect());
        this.lastActivity = System.currentTimeMillis();
        return true;
    }
    
    public Port getPort(String portId) {
        return ports.get(portId);
    }
    
    public List<Port> getAllPorts() {
        return new ArrayList<>(ports.values());
    }
    
    public List<Port> getConnectedPorts() {
        return ports.values().stream()
            .filter(Port::isConnected)
            .collect(Collectors.toList());
    }
    
    public String getNeId() {
        return neId;
    }
    
    public String getNeType() {
        return neType;
    }
    
    public String getLocation() {
        return location;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
    
    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
}
