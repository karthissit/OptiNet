package com.optinet.domain.model;

/**
 * Status of a KPI metric relative to its thresholds.
 * 
 * Used to determine if a metric is within acceptable range (NORMAL),
 * approaching limits (WARNING), or breached (CRITICAL).
 * 
 * This status triggers alarm generation when transitioning to WARNING or CRITICAL.
 */
public enum KpiThresholdStatus {
    NORMAL,      // Metric is within acceptable range
    WARNING,     // Metric is approaching threshold; not yet critical
    CRITICAL     // Metric has exceeded threshold; action required
}
