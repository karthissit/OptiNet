package com.optinet.domain.model;

/**
 * Status of an SLA record.
 * 
 * Indicates whether the service level agreement was met during the measurement window.
 */
public enum SlaStatus {
    MET,      // Achieved value meets or exceeds target
    VIOLATED  // Achieved value fell below target
}
