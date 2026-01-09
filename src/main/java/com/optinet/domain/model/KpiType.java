package com.optinet.domain.model;

/**
 * Enumeration of all KPI types in the optical network domain.
 * 
 * Each KPI type has:
 * - A unique identifier (enum name)
 * - A unit of measurement (dB, mW, %, etc.)
 * - Expected thresholds (defined in KpiThresholdEvaluator)
 * 
 * Using enum instead of strings ensures type safety and prevents typos.
 * Examples: "SNR" vs "snr" vs "signal_noise_ratio" will not happen.
 */
public enum KpiType {
    // Optical transmission metrics (Layer 1)
    OPTICAL_POWER("dBm"),                    // Launch power from transponder or amplifier
    SIGNAL_NOISE_RATIO("dB"),                // SNR: key indicator of signal quality
    CHROMATIC_DISPERSION("ps/nm"),           // Dispersion compensation
    POLARIZATION_MODE_DISP("ps"),            // PMD: polarization effect
    
    // Electrical/digital metrics (Layer 0-1)
    BIT_ERROR_RATE("errors/sec"),            // BER: count of bit errors per second
    Q_FACTOR("linear"),                      // Q-factor: signal quality metric
    PRE_FEC_BER("errors/sec"),               // Before forward error correction
    POST_FEC_BER("errors/sec"),              // After forward error correction
    
    // Environmental metrics (Equipment health)
    TEMPERATURE("°C"),                       // Internal temperature
    HUMIDITY("% RH"),                        // Relative humidity in equipment housing
    
    // Transceiver-specific metrics
    LASER_WAVELENGTH("nm"),                  // Operating wavelength
    TRANSCEIVER_POWER("mW"),                 // Power consumption
    LASER_BIAS_CURRENT("mA"),                // Laser bias current
    
    // Amplifier-specific metrics
    ASE_NOISE("dBm"),                        // Amplified spontaneous emission
    AMPLIFIER_GAIN("dB"),                    // Gain of the amplifier
    AMPLIFIER_NOISE_FIGURE("dB"),            // Noise figure
    
    // Optical switch/cross-connect metrics
    INSERTION_LOSS("dB"),                    // Loss through the switch
    POLARIZATION_DEPENDENT_LOSS("dB"),       // PDL in the switch
    ISOLATION("dB"),                         // Crosstalk isolation between ports
    
    // System/operational metrics
    THROUGHPUT("Gbps"),                      // Data throughput
    LATENCY("ms"),                           // Packet latency
    PACKET_LOSS_RATE("%"),                   // Percentage of lost packets
    AVAILABILITY("%");                       // System availability percentage
    
    private final String unit;
    
    KpiType(String unit) {
        this.unit = unit;
    }
    
    public String unit() {
        return unit;
    }
    
    /**
     * Get a short description of this KPI type.
     */
    public String description() {
        return switch(this) {
            case OPTICAL_POWER -> "Optical signal power level";
            case SIGNAL_NOISE_RATIO -> "Signal-to-noise ratio (SNR)";
            case CHROMATIC_DISPERSION -> "Chromatic dispersion compensation";
            case POLARIZATION_MODE_DISP -> "Polarization mode dispersion";
            case BIT_ERROR_RATE -> "Bit error rate (BER)";
            case Q_FACTOR -> "Signal quality metric (Q-factor)";
            case PRE_FEC_BER -> "Bit error rate before FEC";
            case POST_FEC_BER -> "Bit error rate after FEC";
            case TEMPERATURE -> "Equipment internal temperature";
            case HUMIDITY -> "Relative humidity";
            case LASER_WAVELENGTH -> "Operating wavelength";
            case TRANSCEIVER_POWER -> "Transceiver power consumption";
            case LASER_BIAS_CURRENT -> "Laser bias current";
            case ASE_NOISE -> "Amplified spontaneous emission noise";
            case AMPLIFIER_GAIN -> "Amplifier gain";
            case AMPLIFIER_NOISE_FIGURE -> "Amplifier noise figure";
            case INSERTION_LOSS -> "Switch/cross-connect insertion loss";
            case POLARIZATION_DEPENDENT_LOSS -> "Polarization-dependent loss";
            case ISOLATION -> "Crosstalk isolation";
            case THROUGHPUT -> "Data throughput";
            case LATENCY -> "Packet latency";
            case PACKET_LOSS_RATE -> "Percentage of lost packets";
            case AVAILABILITY -> "System availability";
        };
    }
}
