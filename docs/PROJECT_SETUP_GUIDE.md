# OptiNet Capstone Project — Complete Setup & Architecture Guide

**Version 1.0** | January 2026

---

## Executive Summary

OptiNet is a real-world, enterprise-grade optical network telemetry and management system built as a learning capstone. This document provides complete guidance on:

1. **Project Structure** — Multi-module architecture
2. **Phase 1 Design** — Monolithic telemetry API
3. **Network Element Simulator** — NETCONF-based NE simulation
4. **Getting Started** — Build, run, test
5. **Next Steps** — Path to production-ready system

---

## Project Architecture

### Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      OptiNet System                              │
│                  (Multi-Module Gradle Project)                   │
└─────────────────────────────────────────────────────────────────┘

LAYER 1: NETWORK ELEMENTS (Network Element Simulator Module)
─────────────────────────────────────────────────────────────
[Transponder-NYC] ←→ [Amplifier] ←→ [OXC] ←→ [Amplifier] ←→ [Transponder-BOS]
         │                                                          │
         └──────────── NETCONF/SSH (Port 8830) ────────────────────┘

LAYER 2: MANAGEMENT API (OptiNet API Module - Phase 1)
──────────────────────────────────────────────────────
         │
         ├─→ [REST Controller] (Port 8080)
         │       ├─ POST /api/v1/telemetry
         │       ├─ GET  /api/v1/network-elements/{neId}
         │       └─ GET  /api/v1/alarms
         │
         ├─→ [Business Logic Services]
         │       ├─ TelemetryService (threshold evaluation)
         │       ├─ NeService (NE management)
         │       └─ AlarmService (alarm generation)
         │
         └─→ [Data Access Layer]
                 └─ Spring Data JPA Repositories

LAYER 3: PERSISTENCE
──────────────────
         │
         └─→ [PostgreSQL Database]
                 ├─ network_elements table
                 ├─ kpi_metrics table (partitioned by date)
                 ├─ alarms table
                 └─ sla_records table

LAYER 4 (Phase 2+): EVENT STREAM
─────────────────────────────────
         │
         └─→ [Apache Kafka]
                 └─ Topic: network.events

LAYER 5 (Phase 3+): REAL-TIME ANALYTICS
────────────────────────────────────────
         │
         └─→ [Apache Pinot]
                 └─ Metrics analytics & time-series queries
```

---

## Module Breakdown

### 1. optinet-core
**Pure domain model — no framework dependencies**

```
optinet-core/
├── domain/
│   ├── model/
│   │   ├── NetworkElement.java (record)
│   │   ├── KpiMetric.java (record)
│   │   ├── Alarm.java (record)
│   │   ├── SlaRecord.java (record)
│   │   ├── KpiType.java (enum with 25+ types)
│   │   ├── NeType.java (enum)
│   │   ├── NeStatus.java (enum)
│   │   ├── AlarmSeverity.java (enum)
│   │   ├── AlarmStatus.java (enum)
│   │   ├── KpiThresholdStatus.java (enum)
│   │   ├── SlaMetric.java (enum)
│   │   └── SlaStatus.java (enum)
│   │
│   └── sample/
│       └── SampleNetworkElementSimulator.java (test data generation)
```

**Key Classes:**
- **Records (immutable):** NetworkElement, KpiMetric, Alarm, SlaRecord
- **Enums (type-safe):** KpiType, NeType, AlarmSeverity, etc.
- **Sample Generator:** Creates realistic test data

**Use:** Reused by all other modules (API, Simulator, future microservices)

---

### 2. network-element-simulator
**Simulates real optical network elements with NETCONF support**

```
network-element-simulator/
├── NeSimulatorApplication.java          [Spring Boot entry]
├── netconf/
│   ├── NetconfRpc.java                  [RPC message model]
│   ├── NetconfSession.java              [Client session]
│   └── RpcHandler.java                  [RPC processor]
├── topology/
│   ├── NetworkTopology.java             [Graph of NEs]
│   ├── SimulatedNetworkElement.java     [NE with ports]
│   ├── Port.java (record)               [Port abstraction]
│   ├── PortType.java (enum)             [OPTICAL, ELECTRICAL, etc.]
│   └── TopologyBuilder.java             [5 predefined topologies]
├── auth/
│   └── AuthenticationManager.java       [SSH user auth]
├── config/
│   └── SimulatorConfiguration.java      [Spring config]
├── api/
│   └── SimulatorManagementController.java  [REST API]
└── resources/
    └── application.yml                  [Configuration]
```

**Topologies Provided:**
1. **Point-to-Point:** Simple 2 NE path
2. **Long-Haul:** Multi-span with amplifiers
3. **Mesh:** Multiple paths with cross-connects
4. **Ring:** Protection ring for APS
5. **Complex:** Realistic backbone mix

**Features:**
- REST API (port 8080): Topology queries, port control
- NETCONF/SSH (port 8830): Protocol simulation (framework only)
- Realistic signal degradation modeling
- Extensible port system

---

### 3. optinet-api
**Main telemetry & management API (Phase 1 - to be implemented)**

```
optinet-api/
├── OptiNetApiApplication.java
├── api/
│   ├── controller/
│   │   ├── TelemetryController.java    [POST /api/v1/telemetry]
│   │   ├── NeController.java           [GET /api/v1/ne/{id}]
│   │   └── AlarmController.java        [GET /api/v1/alarms]
│   ├── dto/
│   │   ├── TelemetryRequest.java
│   │   ├── KpiMetricResponse.java
│   │   └── NeHealthDto.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── Custom exceptions
├── domain/
│   ├── repository/
│   │   ├── NetworkElementRepository.java
│   │   ├── KpiMetricRepository.java
│   │   ├── AlarmRepository.java
│   │   └── SlaRecordRepository.java
│   ├── service/
│   │   ├── TelemetryService.java       [Core business logic]
│   │   ├── NeService.java
│   │   ├── AlarmService.java
│   │   └── SlaService.java
│   └── entity/
│       ├── NetworkElementEntity.java   [JPA entity]
│       ├── KpiMetricEntity.java
│       └── AlarmEntity.java
├── infrastructure/
│   ├── config/
│   │   ├── WebConfig.java
│   │   └── JpaConfig.java
│   └── persistence/
│       └── converter/
│           └── KpiTypeConverter.java
└── resources/
    └── application.yml
```

**Database Schema (PostgreSQL):**
- `network_elements` — NE master data
- `kpi_metrics` — Time-series metrics (partitioned)
- `alarms` — Events
- `sla_records` — Compliance tracking

---

## Getting Started

### Prerequisites

```bash
# Java 17+
java -version

# Gradle 7.0+
gradle --version

# (Optional) Docker for PostgreSQL
docker --version
```

### Build All Modules

```bash
cd /workspaces/OptiNet

# Clean and build
gradle clean build

# Output:
# ✓ optinet-core-1.0.0-SNAPSHOT.jar
# ✓ network-element-simulator.jar
# ✓ optinet-api.jar
```

### Run Network Element Simulator

```bash
gradle bootRun -p network-element-simulator

# Output:
# 2024-01-15 10:30:45 INFO  NeSimulatorApplication - Started in 3.5s
# 2024-01-15 10:30:45 INFO  - Initialized NetworkTopology with 7 NEs
# 2024-01-15 10:30:45 INFO  - REST API available at http://localhost:8080
# 2024-01-15 10:30:45 INFO  - NETCONF server on 0.0.0.0:8830 (SSH)
```

### Test Simulator REST API

```bash
# Get topology
curl http://localhost:8080/api/simulator/topology | jq

# Get specific NE
curl http://localhost:8080/api/simulator/ne/OTN-TXP-NYC-001 | jq

# Health check
curl http://localhost:8080/api/simulator/health | jq

# Connect ports
curl -X POST "http://localhost:8080/api/simulator/connect?fromNeId=OTN-TXP-NYC-001&fromPortId=eth1&toNeId=OTN-TXP-BOS-001&toPortId=eth1&degradationDb=5"
```

### Test NETCONF (Future - SSH Support)

```bash
# When SSH server is implemented:
ssh -p 8830 admin@localhost
# Password: admin

# Send NETCONF RPC:
# <rpc message-id="1"><get/></rpc>
# ]]>]]>
```

---

## Phase 1 Design Summary

### What Phase 1 Builds

A **monolithic Spring Boot REST API** that:
1. Receives KPI telemetry from network elements
2. Stores metrics in PostgreSQL
3. Evaluates thresholds and generates alarms
4. Provides queries for NE health and history
5. Tracks SLA compliance

### Architecture Diagram

```
Network Elements → POST /api/v1/telemetry
                        ↓
                  [Spring REST Controller]
                  (input validation)
                        ↓
                  [TelemetryService]
                  (business logic, threshold evaluation)
                        ↓
                  [JPA Repositories]
                  (data access)
                        ↓
                  [PostgreSQL]
                  (persistence)
```

### Key APIs (Phase 1)

**POST /api/v1/telemetry**
```json
{
  "neId": "OTN-TXP-NYC-001",
  "kpiType": "SIGNAL_NOISE_RATIO",
  "value": 18.5,
  "timestamp": 1705318245000
}
```

**GET /api/v1/network-elements/{neId}**
```json
{
  "neId": "OTN-TXP-NYC-001",
  "neName": "Transponder Unit 1",
  "neType": "TRANSPONDER",
  "status": "OPERATIONAL",
  "recentMetrics": [
    { "kpiType": "SNR", "value": 18.5, "thresholdStatus": "NORMAL" },
    { "kpiType": "POWER", "value": -8.2, "thresholdStatus": "NORMAL" }
  ],
  "activeAlarms": []
}
```

**GET /api/v1/alarms?severity=CRITICAL&status=ACTIVE**
```json
{
  "totalCount": 2,
  "alarms": [
    {
      "alarmId": "uuid",
      "neId": "OTN-AMP-001",
      "severity": "CRITICAL",
      "alarmText": "SNR fell below 12 dB",
      "status": "ACTIVE"
    }
  ]
}
```

### Database Schema

**network_elements**
```sql
CREATE TABLE network_elements (
    ne_id VARCHAR(100) PRIMARY KEY,
    ne_name VARCHAR(255),
    ne_type VARCHAR(50),
    status VARCHAR(50),
    last_heartbeat BIGINT,
    created_at TIMESTAMP
);
```

**kpi_metrics** (partitioned by date)
```sql
CREATE TABLE kpi_metrics (
    metric_id UUID PRIMARY KEY,
    ne_id VARCHAR(100) REFERENCES network_elements(ne_id),
    kpi_type VARCHAR(100),
    value NUMERIC(12, 4),
    threshold_status VARCHAR(50),
    timestamp BIGINT,
    created_at TIMESTAMP
);
CREATE INDEX idx_kpi_ne_timestamp ON kpi_metrics(ne_id, timestamp DESC);
```

**alarms**
```sql
CREATE TABLE alarms (
    alarm_id UUID PRIMARY KEY,
    ne_id VARCHAR(100) REFERENCES network_elements(ne_id),
    severity VARCHAR(50),
    alarm_text TEXT,
    occurred_at BIGINT,
    cleared_at BIGINT,
    status VARCHAR(50),
    created_at TIMESTAMP
);
```

---

## Learning Path

### Week 1: Foundation
- [ ] Read `phase-1-design.md` (architecture)
- [ ] Read `DOMAIN_MODEL.md` (optical networking concepts)
- [ ] Run network element simulator
- [ ] Explore REST API responses

### Week 2: Core Domain
- [ ] Study domain records (NetworkElement, KpiMetric, etc.)
- [ ] Understand KPI types and thresholds
- [ ] Study TopologyBuilder (how topologies work)
- [ ] Run `SampleNetworkElementSimulator` to see test data

### Week 3: Phase 1 Implementation
- [ ] Design PostgreSQL schema
- [ ] Create JPA entities and repositories
- [ ] Implement TelemetryService (core logic)
- [ ] Build REST controllers
- [ ] Write unit tests

### Week 4: Integration & Testing
- [ ] Docker Compose setup (PostgreSQL)
- [ ] End-to-end testing
- [ ] Integration with simulator
- [ ] Performance optimization

### Week 5+: Phase 2 (Kafka)
- [ ] Introduce Apache Kafka
- [ ] Decouple telemetry ingestion
- [ ] Add asynchronous processing
- [ ] Message retry/DLQ patterns

---

## Technology Stack

### Current (Phase 1)

| Layer | Technology | Why |
|---|---|---|
| **Framework** | Spring Boot 3.2 | Industry standard, observability |
| **Language** | Java 17+ | Modern syntax, records, sealed classes |
| **Build** | Gradle | Multi-module support, Kotlin DSL |
| **Database** | PostgreSQL | ACID guarantees, rich query language |
| **Testing** | JUnit 5 | Modern, Spring integration |

### Phase 2+

| Layer | Technology | Purpose |
|---|---|---|
| **Message Queue** | Apache Kafka | Decouple, replay, retention |
| **Stream Processing** | Kafka Streams | Real-time event processing |
| **Real-Time Analytics** | Apache Pinot | Time-series queries at scale |
| **Distributed Tracing** | Jaeger | Observability across services |
| **Monitoring** | Prometheus + Grafana | Metrics and dashboards |

---

## Design Principles

### 1. Design-First Development
- Start with architecture documents
- Understand domain before coding
- Design data models carefully

### 2. Immutability & Thread Safety
- Use records for domain models
- Make data structures immutable where possible
- Avoid shared mutable state

### 3. Layered Architecture
```
Controllers (API) → Services (Business Logic) → Repositories (Data Access)
```
- Clear separation of concerns
- Testable in isolation
- Easy to extend

### 4. Event-Driven Future
- Design for eventual Kafka integration
- Avoid tight coupling
- Think about message formats early

### 5. Operational Excellence
- Structured logging
- Clear error messages
- Graceful degradation

---

## Common Pitfalls (From Experienced Engineers)

❌ **Mistake:** Mixing JPA entities with domain records
✅ **Solution:** Keep records immutable; convert to/from entities in service layer

❌ **Mistake:** Using arbitrary strings for KPI types
✅ **Solution:** Enum-driven (type-safe) KPI system

❌ **Mistake:** Storing timestamps in database TIMESTAMP format
✅ **Solution:** Store Unix milliseconds (BIGINT) for consistent time-series queries

❌ **Mistake:** No input validation on REST API
✅ **Solution:** Validate in controller; clear error messages

❌ **Mistake:** Monolith directly coupled to database
✅ **Solution:** Introduce Kafka in Phase 2 for decoupling

---

## Next Steps

### Immediate (This Week)
1. ✅ Review project structure and documentation
2. ✅ Run network element simulator
3. ✅ Explore REST API and network topology
4. [ ] Set up PostgreSQL (Docker or local)
5. [ ] Clone optinet-api module structure

### Short Term (This Month)
1. [ ] Implement Phase 1 API (telemetry ingestion)
2. [ ] Create database schema and migrations (Flyway)
3. [ ] Build services and business logic
4. [ ] Write comprehensive tests
5. [ ] Docker Compose setup for full stack

### Medium Term (Q1 2026)
1. [ ] Phase 2: Introduce Kafka
2. [ ] Asynchronous telemetry processing
3. [ ] Event-driven architecture
4. [ ] Message replay capability

### Long Term (Q2-Q3 2026)
1. [ ] Phase 3: Apache Pinot integration
2. [ ] Real-time analytics queries
3. [ ] Phase 4: Microservices decomposition
4. [ ] Phase 5: Production resilience patterns

---

## Directory Structure (Complete)

```
OptiNet/
├── README.md
├── settings.gradle                    (module definitions)
├── build.gradle                       (root config)
│
├── optinet-core/                      (Pure Java domain module)
│   ├── build.gradle
│   └── src/main/java/com/optinet/domain/
│       ├── model/                     (Records: NetworkElement, KpiMetric, etc.)
│       │   ├── NetworkElement.java
│       │   ├── KpiMetric.java
│       │   ├── Alarm.java
│       │   ├── SlaRecord.java
│       │   └── *Type.java, *Status.java (enums)
│       └── sample/
│           └── SampleNetworkElementSimulator.java
│
├── network-element-simulator/         (NETCONF NE Simulator)
│   ├── build.gradle
│   ├── src/main/java/com/optinet/simulator/
│   │   ├── NeSimulatorApplication.java
│   │   ├── netconf/                   (NETCONF protocol)
│   │   │   ├── NetconfRpc.java
│   │   │   ├── NetconfSession.java
│   │   │   └── RpcHandler.java
│   │   ├── topology/                  (Network topology)
│   │   │   ├── NetworkTopology.java
│   │   │   ├── SimulatedNetworkElement.java
│   │   │   ├── Port.java
│   │   │   ├── PortType.java
│   │   │   └── TopologyBuilder.java
│   │   ├── auth/                      (Authentication)
│   │   │   └── AuthenticationManager.java
│   │   ├── config/                    (Spring config)
│   │   │   └── SimulatorConfiguration.java
│   │   └── api/                       (REST API)
│   │       └── SimulatorManagementController.java
│   └── src/main/resources/
│       └── application.yml
│
├── optinet-api/                       (Phase 1 REST API - to implement)
│   ├── build.gradle
│   └── src/main/java/com/optinet/api/
│       ├── OptiNetApiApplication.java
│       ├── api/controller/
│       ├── domain/service/
│       └── domain/entity/
│
└── docs/
    ├── README.md (this file)
    ├── phase-1-design.md               (Complete Phase 1 architecture)
    ├── network-element-simulator-guide.md
    ├── network-element-simulator-implementation.md
    ├── getting-started.md
    └── DOMAIN_MODEL.md
```

---

## References & Resources

**Optical Networking:**
- ITU-T G.6xx series (transmission standards)
- DWDM Technology (Ciena, Nokia, Infinera)
- Optical network fundamentals

**Java & Spring:**
- [Java 17 Language Features](https://docs.oracle.com/en/java/javase/17/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

**NETCONF:**
- [RFC 6241 - NETCONF 1.0](https://tools.ietf.org/html/rfc6241)
- [NETCONF Operations](https://tools.ietf.org/html/rfc6241#section-7)

**Architecture:**
- Domain-Driven Design (Eric Evans)
- Microservices Patterns (Sam Newman)
- Designing Data-Intensive Applications (Martin Kleppmann)

---

## Quick Reference: Key Files

| File | Purpose |
|---|---|
| `settings.gradle` | Module definitions (optinet-core, network-element-simulator, optinet-api) |
| `build.gradle` | Root project config, dependency management |
| `optinet-core/build.gradle` | Core module (no Spring) |
| `network-element-simulator/build.gradle` | Simulator module |
| `optinet-api/build.gradle` | API module (Phase 1) |
| `docs/phase-1-design.md` | Complete Phase 1 architecture |
| `docs/network-element-simulator-guide.md` | Simulator usage guide |
| `docs/DOMAIN_MODEL.md` | Optical networking concepts |

---

**Version 1.0 Complete** ✅

**Next update:** After Phase 1 implementation

**Questions?** Refer to documentation or review code comments.

---

**Let's build enterprise-grade systems.**
