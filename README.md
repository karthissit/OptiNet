# OptiNet: Optical Network Telemetry & Management System

**A Real-World Enterprise-Grade Capstone Project**

## Executive Summary

OptiNet is a production-grade optical network telemetry and management system designed to collect, process, store, and analyze KPIs (Key Performance Indicators) from distributed network elements in real-time. This project demonstrates how to build a scalable, event-driven system for large-scale optical/telecom infrastructure.

**Target Domain:** Optical telecommunications networks (e.g., DWDM, OTDR, transponders, amplifiers, cross-connects)

---

## Project Philosophy

This is **not a tutorial project**. It's a capstone that mirrors real production systems you'd encounter at companies like Ciena, Nokia, Infinera, or Arista. The focus is on:

- **Architecture-first design** before implementation
- **Incremental complexity** that evolves with production requirements
- **Technology depth** with enterprise patterns
- **Clear trade-offs** and decision rationale

---

## Problem Statement

### The Context

In optical networks, thousands of network elements (NEs) continuously generate telemetry data—signal-to-noise ratios, bit error rates, laser power, temperature, alarms, and more. Operators need:

1. **Real-time visibility** into network health (milliseconds matter)
2. **Historical analysis** to detect trends and predict failures
3. **SLA compliance tracking** (latency, availability, bit errors)
4. **Efficient data storage** (millions of metrics per second)
5. **Operational alerting** when thresholds are breached

### The Challenge

Building a system that:
- Ingests high-volume, high-velocity telemetry from heterogeneous NEs
- Processes and correlates events in real-time
- Stores billions of time-series metrics efficiently
- Enables ad-hoc analytics without blocking real-time processing
- Scales horizontally as the network grows

This is your **OptiNet** system.

---

## Architecture Philosophy

**Phase 1 → Phase N:** We evolve from a **monolithic synchronous system** to a **distributed event-driven architecture**.

```
Phase 1: Monolith + Direct Persistence
  ↓
Phase 2: Introduce Event Queue (Kafka)
  ↓
Phase 3: Real-Time Analytics (Apache Pinot)
  ↓
Phase 4: Microservices Decomposition
  ↓
Phase 5: High Availability & Resilience
```

Each phase adds realistic constraints and teaches new patterns.

---

## Technologies & Learning Goals

| Technology | Why | Learning Goals |
|---|---|---|
| **Java 17+** | Modern JVM features, records, sealed classes | Concurrency, streams, best practices |
| **Spring Boot** | Industry standard, fast development, observability | REST, validation, configuration, testing |
| **PostgreSQL** | Relational backbone for topology, configs, SLA | Schema design, indexing, transactions |
| **Apache Kafka** | Event queue for decoupling, replay, persistence | Producers/consumers, partitioning, offsets |
| **Apache Pinot** | Real-time OLAP for metrics & analytics | Ingestion, time-series queries, aggregations |
| **Docker** | Local development, reproducible environments | Containerization, Docker Compose, networking |
| **Gradle** | Build automation, dependency management | Multi-module builds, custom tasks |

---

## Project Structure (Final State)

```
optinet/
├── docs/
│   ├── v1-capstone-proposal.md
│   ├── phase-1-design.md
│   ├── phase-2-design.md
│   └── ...
├── gradle/
│   └── wrapper/
├── src/
│   ├── main/java/com/optinet/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── api/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   ├── infrastructure/
│   │   │   ├── config/
│   │   │   ├── kafka/
│   │   │   └── persistence/
│   │   └── OptiNetApplication.java
│   └── test/java/com/optinet/
├── docker/
│   ├── docker-compose.yml
│   ├── postgres/
│   └── kafka/
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Capstone Phases Overview

### Phase 1: Foundation — Monolithic Telemetry Ingestion (THIS PHASE)
- Model network elements (NEs) and KPIs
- Build REST API for telemetry ingestion
- Persist to PostgreSQL
- Learn: Java records, Spring REST, PostgreSQL basics

### Phase 2: Event-Driven Architecture
- Introduce Kafka for decoupling
- Asynchronous processing
- Learn: Kafka patterns, event design, backpressure

### Phase 3: Real-Time Analytics
- Ingest Kafka events into Apache Pinot
- Build analytics queries
- Learn: Pinot time-series, real-time OLAP

### Phase 4: Microservices Decomposition
- Split monolith into domain services
- Inter-service communication
- Learn: Distributed systems, eventual consistency

### Phase 5: Production Readiness
- Resilience (circuit breakers, retries)
- Observability (metrics, tracing, logging)
- High availability patterns

---

## Expected Outcomes

By the end of Phase 5:
- ✅ Designed and implemented a real-world system end-to-end
- ✅ Understood how Java, Spring, PostgreSQL, Kafka, and Pinot work together
- ✅ Learned enterprise architecture patterns
- ✅ Built something portfolio-worthy for senior roles
- ✅ Have a mental model for designing other systems

---

## How to Use This Project

1. **Read the design docs first** (`docs/phase-1-design.md`) before writing code
2. **Understand the "why"** behind architecture decisions
3. **Build incrementally**—each phase is self-contained but builds on the previous
4. **Reference real patterns** from production systems
5. **Don't skip the difficult parts**—they're where learning happens

---

## Documentation Index

- [Phase 1 Design: Foundation & Telemetry Ingestion](docs/phase-1-design.md)
- (Phase 2, 3, 4, 5 to follow as you progress)

---

**Let's build something substantial.**