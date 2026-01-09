package com.optinet.domain.model;

import java.util.UUID;

/**
 * Represents an alarm raised by the system.
 * 
 * Alarms are triggered when:
 * 1. A KPI exceeds a threshold (via TelemetryService)
 * 2. A network element fails to send heartbeat (via HealthCheckService)
 * 3. Manual intervention is triggered (future: via API)
 * 
 * Alarms drive operational response in a real network:
 * - NOCs (Network Operations Centers) monitor for critical alarms
 * - Automated remediation systems may trigger based on alarm patterns
 * - SLA violations are tracked through alarms
 */
public record Alarm(
    String alarmId,           // UUID: unique identifier
    String neId,              // Network element that triggered the alarm
    AlarmSeverity severity,   // CRITICAL, MAJOR, MINOR, WARNING
    String alarmText,         // Human-readable description
    long occurredAt,          // Unix ms when condition started
    Long clearedAt,           // Unix ms when resolved (null if still active)
    AlarmStatus status        // ACTIVE, ACKNOWLEDGED, CLEARED
) {
    
    /**
     * Create a new active alarm.
     */
    public static Alarm create(
        String neId,
        AlarmSeverity severity,
        String alarmText) {
        return new Alarm(
            UUID.randomUUID().toString(),
            neId,
            severity,
            alarmText,
            System.currentTimeMillis(),
            null,
            AlarmStatus.ACTIVE
        );
    }
    
    /**
     * Create a copy with status updated to ACKNOWLEDGED.
     */
    public Alarm acknowledge() {
        return new Alarm(
            alarmId,
            neId,
            severity,
            alarmText,
            occurredAt,
            clearedAt,
            AlarmStatus.ACKNOWLEDGED
        );
    }
    
    /**
     * Create a copy with status updated to CLEARED.
     */
    public Alarm clear() {
        return new Alarm(
            alarmId,
            neId,
            severity,
            alarmText,
            occurredAt,
            System.currentTimeMillis(),
            AlarmStatus.CLEARED
        );
    }
    
    /**
     * Check if alarm is currently active.
     */
    public boolean isActive() {
        return status == AlarmStatus.ACTIVE || status == AlarmStatus.ACKNOWLEDGED;
    }
}
