package com.optinet.domain.model;

/**
 * Severity level of an alarm.
 * 
 * Used to prioritize operational response:
 * - CRITICAL: Service-impacting; requires immediate action
 * - MAJOR: Significant degradation; should be addressed soon
 * - MINOR: Functional but not ideal; monitor for escalation
 * - WARNING: Informational; for trend monitoring
 */
public enum AlarmSeverity {
    CRITICAL,     // Service impact; potential customer-facing issue
    MAJOR,        // Significant degradation; may lead to service impact
    MINOR,        // Minor issue; does not impact service
    WARNING       // Informational; for awareness and trend analysis
}
