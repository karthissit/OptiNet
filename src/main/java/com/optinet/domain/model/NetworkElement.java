package com.optinet.domain.model;

/**
 * Represents a network element in the optical network.
 * 
 * A network element is any physical or logical device that:
 * - Generates telemetry data (KPIs)
 * - Can raise alarms
 * - Is subject to SLA tracking
 * 
 * Examples: transponders, amplifiers, cross-connects, optical switches, modems.
 */
public record NetworkElement(
    String neId,              // Unique identifier (e.g., "OTN-TXP-001")
    String neName,            // Human-readable name (e.g., "Transponder Unit 1")
    NeType neType,            // Type of device (TRANSPONDER, AMPLIFIER, etc.)
    String neModel,           // Equipment model (e.g., "Infinera-XTM")
    String location,          // Physical location (e.g., "NYC-DC-1")
    NeStatus status,          // Current operational status
    long lastHeartbeat,       // Unix ms of last telemetry received
    long createdAt,           // Unix ms when registered
    long updatedAt            // Unix ms when last updated
) {
    
    /**
     * Create a new network element with current timestamp.
     */
    public static NetworkElement create(
        String neId,
        String neName,
        NeType neType,
        String neModel,
        String location) {
        long now = System.currentTimeMillis();
        return new NetworkElement(
            neId,
            neName,
            neType,
            neModel,
            location,
            NeStatus.OPERATIONAL,
            now,
            now,
            now
        );
    }
    
    /**
     * Create a copy with updated status.
     */
    public NetworkElement withStatus(NeStatus newStatus) {
        return new NetworkElement(
            neId,
            neName,
            neType,
            neModel,
            location,
            newStatus,
            lastHeartbeat,
            createdAt,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Create a copy with updated heartbeat timestamp.
     */
    public NetworkElement withHeartbeat(long newHeartbeat) {
        return new NetworkElement(
            neId,
            neName,
            neType,
            neModel,
            location,
            status,
            newHeartbeat,
            createdAt,
            System.currentTimeMillis()
        );
    }
}
