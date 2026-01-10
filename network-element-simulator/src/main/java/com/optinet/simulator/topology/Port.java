package com.optinet.simulator.topology;

/**
 * Represents a physical or logical port on a network element.
 * Ports enable network elements to connect and exchange data.
 * Types:
 * - Optical: Wavelength-specific (e.g., 1550nm on DWDM)
 * - Electrical: Client-side (e.g., 100Gbps Ethernet)
 * - Control: Management/monitoring
 */
public record Port(
    String portId,                // Unique ID: "eth0", "optical-1", etc.
    String portName,              // Human-readable name
    PortType portType,            // OPTICAL, ELECTRICAL, CONTROL
    String connectionStatus,      // AVAILABLE, CONNECTED, DOWN
    String connectedNeId,         // Which NE is connected (null if none)
    String connectedPortId,       // Which port on connected NE
    double transmitPower,         // For optical: TX power in dBm
    double receivePower,          // For optical: RX power in dBm
    String wavelength             // For optical: wavelength (e.g., "1550nm")
) {
    
    public static Port createOpticalPort(
        String portId,
        String portName,
        double txPower,
        String wavelength) {
        return new Port(
            portId,
            portName,
            PortType.OPTICAL,
            "AVAILABLE",
            null,
            null,
            txPower,
            -99.0,  // RX not connected
            wavelength
        );
    }
    
    public static Port createElectricalPort(
        String portId,
        String portName) {
        return new Port(
            portId,
            portName,
            PortType.ELECTRICAL,
            "AVAILABLE",
            null,
            null,
            0.0,
            0.0,
            null
        );
    }
    
    public boolean isConnected() {
        return "CONNECTED".equals(connectionStatus) && connectedNeId != null;
    }
    
    /**
     * Create a connected copy of this port
     */
    public Port withConnection(String remoteNeId, String remotePortId, double rxPower) {
        return new Port(
            portId,
            portName,
            portType,
            "CONNECTED",
            remoteNeId,
            remotePortId,
            transmitPower,
            rxPower,
            wavelength
        );
    }
    
    /**
     * Create a disconnected copy of this port
     */
    public Port disconnect() {
        return new Port(
            portId,
            portName,
            portType,
            "AVAILABLE",
            null,
            null,
            transmitPower,
            -99.0,
            wavelength
        );
    }
}
