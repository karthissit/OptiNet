package com.optinet.simulator.topology;

/**
 * Type of network port
 */
public enum PortType {
    OPTICAL,       // Wavelength-specific optical (DWDM, OTN)
    ELECTRICAL,    // Client-side Ethernet
    CONTROL,       // Management/OAM port
    TRIBUTARY      // Sub-wavelength tributary
}
