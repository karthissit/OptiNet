package com.optinet.domain.model;

/**
 * Operational status of a network element.
 * 
 * Reflects the element's ability to function and carry traffic.
 * Status is determined by:
 * - Absence of critical alarms
 * - Health of key KPIs (SNR, BER, power, etc.)
 * - Recent heartbeat (telemetry) communication
 */
public enum NeStatus {
    OPERATIONAL,   // Fully functional; all KPIs within range
    DEGRADED,      // Functional but with issues; some KPIs approaching threshold
    FAILED,        // Non-functional; critical alarms present
    UNKNOWN        // No recent telemetry; unable to determine status
}
