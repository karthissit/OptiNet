# OptiNet Domain Model Glossary

A reference guide to optical networking and OptiNet concepts.

---

## Core Concepts

### Network Element (NE)
A physical or logical device in an optical network that generates telemetry and is subject to monitoring.

**Examples:**
- Transponder: Converts electrical to optical signals
- Amplifier (EDFA): Boosts optical signals
- Optical Cross-Connect (OXC): Routes wavelengths
- Optical Switch: Routes between fiber paths

**Characteristics:**
- Has unique ID (neId): e.g., "OTN-TXP-NYC-001"
- Has type: TRANSPONDER, AMPLIFIER, SWITCH, etc.
- Generates KPI metrics periodically
- Can raise alarms when thresholds breach
- Has operational status: OPERATIONAL, DEGRADED, FAILED, UNKNOWN

### KPI (Key Performance Indicator)
A quantifiable measurement from a network element at a specific point in time.

**Common KPIs:**
- **Optical Domain:**
  - OPTICAL_POWER: Signal strength (dBm)
  - SIGNAL_NOISE_RATIO (SNR): Signal quality (dB)
  - BIT_ERROR_RATE (BER): Data corruption (errors/sec)
  - Q_FACTOR: Signal quality metric (linear)

- **Environmental:**
  - TEMPERATURE: Equipment temperature (°C)
  - HUMIDITY: Relative humidity (% RH)

- **Operational:**
  - THROUGHPUT: Data rate (Gbps)
  - LATENCY: Packet delay (ms)
  - AVAILABILITY: Uptime percentage (%)

### Alarm
An event triggered when a KPI crosses thresholds or system health degrades.

**Severity Levels:**
- **CRITICAL:** Service-impacting; requires immediate action
- **MAJOR:** Significant degradation; should be addressed
- **MINOR:** Non-critical issue; monitor for escalation
- **WARNING:** Informational; for trend awareness

**Status:**
- **ACTIVE:** Condition exists; unacknowledged
- **ACKNOWLEDGED:** Operator aware; working on fix
- **CLEARED:** Resolved

**Example:** SNR drops below 12 dB → CRITICAL alarm → NOC (Network Operations Center) responds

### SLA (Service Level Agreement)
Contractual guarantee of service quality.

**Typical SLA Metrics:**
- **AVAILABILITY:** 99.99% uptime (52 minutes downtime/year)
- **MEAN_TIME_BETWEEN_FAILURES:** Hours between failures
- **MEAN_TIME_TO_RECOVERY:** Hours to fix after failure
- **BIT_ERROR_RATE:** Max tolerable BER
- **LATENCY:** Max acceptable packet delay
- **THROUGHPUT:** Min guaranteed data rate

**Usage:** OptiNet measures achieved values vs. targets and reports compliance (MET or VIOLATED).

---

## Optical Networking Primer

### Optical Transmission

**DWDM (Dense Wavelength Division Multiplexing):**
- Combines multiple wavelengths (colors) onto a single fiber
- Enables multi-terabit capacity on one fiber pair
- Each wavelength is independent (different data streams)

**Common Wavelengths (C-Band):**
- 1530 nm to 1565 nm
- Spaced 0.1-0.8 nm apart (100+ channels possible)

### Key Components

**Transponder (Client-to-Line Card):**
```
[Client Electrical Signal 100 Gbps]
         ↓
[Convert to Optical at 1550nm]
         ↓
[Launch at -8 dBm (power)]
```

**Amplifier (EDFA - Erbium-Doped Fiber Amplifier):**
```
[Weak Optical Signal: -20 dBm]
         ↓
[Amplify by ~25-30 dB]
         ↓
[Strong Signal: +3 to +5 dBm]
```

**Optical Cross-Connect (OXC):**
```
Input Wavelengths: λ1, λ2, λ3, λ4
         ↓
[Demultiplex into separate wavelengths]
         ↓
[Select: λ1→Port1, λ2→Port3, λ3→Port2, λ4→Drop]
         ↓
Output Routes
```

---

## KPI Reference Table

| KPI Type | Unit | Typical Range | Threshold (Normal) | Threshold (Warning) | Threshold (Critical) |
|---|---|---|---|---|---|
| OPTICAL_POWER | dBm | -10 to +5 | -9 to -7 | -10 to -9, -7 to +1 | < -10, > +1 |
| SIGNAL_NOISE_RATIO | dB | 12-22 | ≥ 15 | 12-15 | < 12 |
| BIT_ERROR_RATE | errors/sec | 1e-15 to 1e-8 | < 1e-12 | 1e-12 to 1e-11 | > 1e-11 |
| TEMPERATURE | °C | 0-80 | 20-60 | 60-70 | > 70 |
| AMPLIFIER_GAIN | dB | 20-35 | 25-35 | 20-25, 35-38 | < 20, > 38 |
| AMPLIFIER_NOISE_FIGURE | dB | 3-7 | ≤ 5 | 5-6 | > 6 |
| INSERTION_LOSS | dB | 1-8 | 2-4 | 4-6 | > 6 |
| ISOLATION | dB | 40-80 | ≥ 60 | 40-60 | < 40 |

---

## Real-World Scenario: Network Path

### Long-Haul Optical Network Path

```
New York (Sender)
    ↓
    [OTN-TXP-NYC-001] ← Transponder (converts electrical → optical)
      KPIs: SNR=18.5dB, Power=-8.2dBm, Temp=45°C
    ↓
    [EDFA-NYC-001] ← Amplifier (boosts signal)
      KPIs: Gain=28.5dB, NF=4.8dB, Output=+17.2dBm
    ↓
    [Fiber Span: 80 km] ← Signal degrades over distance
    ↓
    [OXC-BOSTON-001] ← Cross-Connect (wavelength routing)
      KPIs: IL=3.2dB, Isolation=65dB
    ↓
    [EDFA-BOSTON-001] ← Amplifier (regenerate)
      KPIs: Gain=28.5dB, Input=-25dBm (degraded from fiber)
    ↓
    [OTN-TXP-BOSTON-001] ← Receiver Transponder
      KPIs: SNR=17.2dB (slightly degraded from original)
    ↓
Boston (Receiver)
```

**What OptiNet Monitors:**
- All KPIs in real-time
- SNR degradation over fiber spans
- Amplifier gain trending (indicates aging)
- Alarms if SNR drops (e.g., fiber cut upstream)
- SLA: BER stays < 1e-12 for 99.99% of time

---

## Threshold Strategy

### Why Thresholds?

Each KPI has acceptable operating ranges:

```
SNR Example:
  ├─ NORMAL:   ≥ 15 dB (good margin, no action needed)
  ├─ WARNING:  12-15 dB (approaching limit; trend toward critical)
  └─ CRITICAL: < 12 dB (margin exhausted; service risk)
```

**Design:**
- **NORMAL threshold:** Comfortable operating range
- **WARNING threshold:** Early warning (trend analysis, predict failures)
- **CRITICAL threshold:** Action required (alarm to NOC)

### Threshold Evaluation in Phase 1

`KpiThresholdEvaluator` service determines status for each metric:

```java
KpiThresholdStatus evaluate(KpiType type, double value) {
    return switch(type) {
        case SIGNAL_NOISE_RATIO -> {
            if (value >= 15.0) yield NORMAL;
            if (value >= 12.0) yield WARNING;
            yield CRITICAL;
        }
        case TEMPERATURE -> {
            if (value <= 60.0) yield NORMAL;
            if (value <= 70.0) yield WARNING;
            yield CRITICAL;
        }
        // ... more KPI types
    };
}
```

---

## Data Collection Patterns

### Real Network Elements (in production)

1. **SNMP Polling:** Periodic requests to device (typically every 30-60 seconds)
2. **Streaming Telemetry:** Device pushes metrics continuously (faster, less overhead)
3. **NETCONF:** Standard configuration/monitoring protocol
4. **Vendor APIs:** Equipment-specific APIs

**OptiNet Phase 1:** Simplifies to REST API for learning
**OptiNet Phase 2+:** Will support adapters for real protocols

### Typical Collection Interval

- **Fast metrics** (BER, power): Every 10-30 seconds
- **Environmental** (temp, humidity): Every 5 minutes
- **Trending metrics** (availability, SLA): Daily aggregations

---

## Common Abbreviations

| Abbreviation | Meaning |
|---|---|
| NE | Network Element |
| KPI | Key Performance Indicator |
| SNR | Signal-to-Noise Ratio |
| BER | Bit Error Rate |
| DWDM | Dense Wavelength Division Multiplexing |
| EDFA | Erbium-Doped Fiber Amplifier |
| OXC | Optical Cross-Connect |
| OTN | Optical Transport Network |
| FEC | Forward Error Correction |
| PDL | Polarization-Dependent Loss |
| PMD | Polarization Mode Dispersion |
| ASE | Amplified Spontaneous Emission |
| QoS | Quality of Service |
| SLA | Service Level Agreement |
| NOC | Network Operations Center |

---

## Relationship Diagram

```
Network Element (1)
    ↓
    └── generates many ─→ KPI Metrics
    └── may have → Alarms
    └── subject to → SLA Records

KPI Metric
    ├── has threshold status → KpiThresholdStatus
    └── can trigger → Alarm

Alarm
    ├── has severity → AlarmSeverity
    └── has status → AlarmStatus

SLA Record
    ├── tracks metric type → SlaMetric
    └── has status → SlaStatus (MET/VIOLATED)
```

---

## Learning Path for Phase 1

1. **Understand NE types** and what they do in optical networks
2. **Learn KPI meanings** (SNR, BER, power) and typical ranges
3. **Grasp threshold logic** (NORMAL, WARNING, CRITICAL)
4. **See alarms as events** (not exceptions, but domain events)
5. **Recognize SLA tracking** as contractual necessity
6. **Map to code:** Records, enums, repositories, services

---

## References

- **Optical Networking Basics:** [Cisco Learning Network](https://learningnetwork.cisco.com/)
- **DWDM Technology:** Ciena, Nokia, Infinera datasheets
- **Telecom Standards:** ITU-T G.6xx series (optical transmission)
- **Domain-Driven Design:** Eric Evans' book

---

**This glossary will expand as you move through Phases 2-5.**
