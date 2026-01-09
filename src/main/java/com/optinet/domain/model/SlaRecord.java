package com.optinet.domain.model;

import java.util.UUID;

/**
 * Represents SLA (Service Level Agreement) compliance for a network element.
 * 
 * SLAs define contractual performance targets:
 * - Availability: typically 99.99% (52 minutes downtime per year)
 * - BER: bit error rate below threshold
 * - Latency: packet delay within bounds
 * 
 * Periodically (daily, weekly, monthly), OptiNet measures achieved values
 * and compares against targets. Violations are tracked for customer reporting
 * and internal capacity planning.
 */
public record SlaRecord(
    String slaId,                // UUID: unique identifier
    String neId,                 // Network element
    SlaMetric metric,            // Type of SLA metric
    double targetValue,          // Contractual target (e.g., 99.99%)
    double achievedValue,        // Measured value during window
    long windowStartTime,        // Unix ms: start of measurement period
    long windowEndTime,          // Unix ms: end of measurement period
    SlaStatus status             // MET or VIOLATED
) {
    
    /**
     * Create a new SLA record.
     */
    public static SlaRecord create(
        String neId,
        SlaMetric metric,
        double targetValue,
        double achievedValue,
        long windowStartTime,
        long windowEndTime) {
        
        SlaStatus status = achievedValue >= targetValue ? 
            SlaStatus.MET : SlaStatus.VIOLATED;
        
        return new SlaRecord(
            UUID.randomUUID().toString(),
            neId,
            metric,
            targetValue,
            achievedValue,
            windowStartTime,
            windowEndTime,
            status
        );
    }
}
