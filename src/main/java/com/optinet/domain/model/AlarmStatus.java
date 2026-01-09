package com.optinet.domain.model;

/**
 * Status of an alarm.
 * 
 * Tracks the lifecycle of an alarm from detection to resolution.
 */
public enum AlarmStatus {
    ACTIVE,        // Condition exists; unacknowledged
    ACKNOWLEDGED,  // Operator has seen the alarm; working on resolution
    CLEARED        // Condition resolved; alarm closed
}
