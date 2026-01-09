package com.optinet.domain.model;

/**
 * Type of network element in an optical network.
 * 
 * Each type has different characteristics and generates different KPIs.
 * Used for filtering, querying, and type-specific logic.
 */
public enum NeType {
    TRANSPONDER,      // Sends/receives optical signals; converts between electrical and optical
    AMPLIFIER,        // Boosts optical signal power; typically EDFA (erbium-doped fiber amplifier)
    SWITCH,           // Routes signals between different paths
    CROSS_CONNECT,    // Wavelength-selective cross-connect; multiplexes/demultiplexes channels
    OPTICAL_MODEM,    // Advanced modulation/demodulation device
    MULTIPLEXER,      // Combines multiple signals onto fewer fibers
    DEMULTIPLEXER,    // Separates signals; opposite of multiplexer
    OPTICAL_REPEATER, // Regenerates degraded signals
    TERMINATION_UNIT, // Terminates fiber connections; common in metro networks
    UNKNOWN           // Fallback for unmapped device types
}
