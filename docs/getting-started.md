# OptiNet Phase 1: Getting Started Guide

## Overview

This guide walks you through understanding and running the Phase 1 foundation of OptiNet.

---

## Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **PostgreSQL 13+** (or Docker)
- **Gradle 7.0+**
- **Docker & Docker Compose** (for local environment)
- **Git**

---

## Project Structure Quick Reference

```
OptiNet/
├── src/main/java/com/optinet/
│   └── domain/
│       ├── model/               # Domain records and enums
│       │   ├── NetworkElement.java
│       │   ├── KpiMetric.java
│       │   ├── Alarm.java
│       │   ├── KpiType.java (enum with 25+ KPI types)
│       │   └── ...enums
│       └── sample/
│           └── SampleNetworkElementSimulator.java
│
└── docs/
    ├── phase-1-design.md        # Detailed architecture and design
    ├── getting-started.md        # This file
    └── DOMAIN_MODEL.md           # Glossary of domain concepts
```

---

## Understanding the Domain Model

### Network Elements (NEs)

A **Network Element** is any device in the optical network that:
- Generates telemetry (KPIs)
- Can raise alarms
- Is subject to SLA tracking

**Types:**
- **Transponder**: Client-to-optical interface (client → optical signal)
- **Amplifier**: Boosts optical signal (EDFA)
- **Cross-Connect (OXC)**: Routes wavelengths
- **Optical Switch**: Routes between fiber paths
- Others: Repeater, Modem, Multiplexer

**Example:** A transponder converts 100 Gbps electrical signals from routers into optical form for transmission.

### KPI Metrics

**KPI** = Key Performance Indicator. A measured value from a network element.

**Examples:**
- **SIGNAL_NOISE_RATIO (SNR)**: Quality of received signal (measured in dB)
- **BIT_ERROR_RATE (BER)**: How many bits are corrupted (errors/sec)
- **OPTICAL_POWER**: Strength of optical signal (dBm)
- **TEMPERATURE**: Equipment internal temperature (°C)

**25+ KPI types** are pre-defined in the enum (see `KpiType.java`).

### Alarms

An **Alarm** is triggered when:
1. A KPI crosses a threshold (e.g., SNR drops below 15 dB)
2. An NE stops sending heartbeats (offline detection)
3. Critical conditions occur

**Severities:**
- **CRITICAL**: Service-impacting (e.g., BER too high)
- **MAJOR**: Significant degradation (e.g., SNR approaching limit)
- **MINOR**: Non-critical but should be monitored
- **WARNING**: Informational

**Status:**
- **ACTIVE**: Condition still exists
- **ACKNOWLEDGED**: Operator aware, working on fix
- **CLEARED**: Resolved

### SLA Records

**SLA** = Service Level Agreement. Contractual performance targets.

**Examples:**
- Availability: 99.99% uptime
- BER: < 1e-12
- Latency: < 50 ms

OptiNet measures achieved values over time windows (daily, weekly, monthly) and reports compliance.

---

## Exploring the Code

### Run the Sample Simulator

The `SampleNetworkElementSimulator` class demonstrates data generation:

```bash
cd /workspaces/OptiNet

# Compile and run the sample
gradle build
gradle runSample
```

**Output:**
```
=== OptiNet Sample Network Element Simulator ===

1. Creating Sample Network Elements:
   - OTN-TXP-NYC-001 (OTN Transponder - OTN-TXP-NYC-001) at NYC-DataCenter-1
   - EDFA-NYC-001 (EDFA Amplifier - EDFA-NYC-001) at NYC-DataCenter-1
   ...

2. Generating Sample KPIs:

   Transponder: OTN-TXP-NYC-001
      - OPTICAL_POWER: -8.20 dBm
      - SIGNAL_NOISE_RATIO: 19.50 dB
      - BIT_ERROR_RATE: 1.0000e-11 errors/sec
      ...
```

This demonstrates:
- How to instantiate NEs programmatically
- What realistic KPI values look like
- The data structures you'll persist to PostgreSQL

### Understanding Records

All domain models use Java 17+ **records** for immutability:

```java
// Before (verbose)
class KpiMetric {
    private String metricId;
    private String neId;
    // ... 20+ lines of getters, setters, equals, hashCode
}

// After (clean)
record KpiMetric(
    String metricId,
    String neId,
    KpiType kpiType,
    double value,
    // ...
) {}
```

**Benefits:**
- Thread-safe (immutable by design)
- Less boilerplate
- Modern Java 17+ best practice
- Perfect for DTOs

---

## Key Concepts Before Phase 1 Implementation

### 1. Synchronous Request-Response

Phase 1 is **synchronous**:

```
Network Element
  ↓ HTTP POST /api/v1/telemetry
  ↓ { neId, kpiType, value, timestamp }
  ↓
Spring Controller
  ↓ Validates input
  ↓
TelemetryService
  ↓ Checks KPI thresholds
  ↓ Creates alarm if needed
  ↓
PostgreSQL (persist)
  ↓
Response: 201 Created
```

**Pro:** Simple, guaranteed consistency
**Con:** Database latency affects response time

**Phase 2** will introduce Kafka for decoupling.

### 2. Threshold Evaluation

Each KPI type has thresholds:

```
SNR (Signal-to-Noise Ratio):
  NORMAL:   ≥ 15 dB
  WARNING:  12-15 dB (approaching limit)
  CRITICAL: < 12 dB
```

When a metric exceeds thresholds, `TelemetryService` creates an alarm.

### 3. NE Status Determination

An NE's status is computed from:
- Absence of CRITICAL alarms
- % of KPIs in NORMAL state
- Recent heartbeat (within 5 minutes?)

---

## Common Mistakes (From Experienced Engineers)

1. **Mixing JPA entities with domain records**
   - ❌ Use `@Entity` on a record (doesn't work)
   - ✅ Keep records immutable; convert to/from JPA entities in service

2. **Storing arbitrary KPI types as strings**
   - ❌ Allow "SNR" vs "snr" vs "signal_noise_ratio"
   - ✅ Use `KpiType` enum; type-safe

3. **Using database timestamps instead of Unix milliseconds**
   - ❌ Store as `TIMESTAMP` (timezone confusion, query inefficiency)
   - ✅ Store as `BIGINT` (Unix ms); easier for time-series

4. **No input validation**
   - ❌ Accept any KPI value from API
   - ✅ Validate range, precision, timestamp not in future

5. **Tight coupling NEs to database**
   - ❌ NE → API → DB with no buffering
   - ✅ Phase 2 will add Kafka queue for resilience

---

## Next: Implementing Phase 1

Once you understand the domain model:

1. **Set up PostgreSQL** with Docker Compose
2. **Create JPA entities** (mutable versions of records)
3. **Implement repositories** (Spring Data JPA)
4. **Build REST controllers** (telemetry ingestion, queries)
5. **Write business logic** in services
6. **Add tests** (unit & integration)

See `phase-1-design.md` for detailed architecture and API specs.

---

## FAQ

**Q: Why records instead of classes?**
A: Immutability is safer in concurrent systems. Records eliminate boilerplate and are the modern Java standard.

**Q: Why PostgreSQL over NoSQL?**
A: Relational model matches our structured data. ACID guarantees prevent data loss. Time-series analytics move to Pinot in Phase 3.

**Q: How do real network elements send data?**
A: Usually via SNMP, NETCONF, or proprietary APIs. Phase 1 simplifies to REST for learning; Phase 2+ will add adapters for real protocols.

**Q: What's the learning arc?**
A: Phase 1 (foundational REST API) → Phase 2 (event-driven with Kafka) → Phase 3 (real-time analytics with Pinot) → Phase 4+ (microservices, resilience).

---

## Resources

- [Java 17 Features](https://docs.oracle.com/en/java/javase/17/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Domain-Driven Design (Eric Evans)](https://en.wikipedia.org/wiki/Domain-driven_design)

---

**Next:** Read `phase-1-design.md` for complete architecture and implementation roadmap.
