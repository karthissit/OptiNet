package com.optinet.domain.model;

/**
 * Types of SLA metrics tracked for network elements.
 * 
 * Each metric has a meaning and a typical target value.
 */
public enum SlaMetric {
    AVAILABILITY,              // % uptime: typically 99.99%
    MEAN_TIME_BETWEEN_FAILURES, // Hours between failures
    MEAN_TIME_TO_RECOVERY,      // Hours to fix after failure
    BIT_ERROR_RATE,             // Max acceptable BER
    LATENCY,                    // Max packet delay in ms
    THROUGHPUT                  // Min guaranteed throughput
}
