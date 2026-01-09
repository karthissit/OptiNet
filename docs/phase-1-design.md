# Phase 1 Design: Foundation & Monolithic Telemetry Ingestion

**Objective:** Build a monolithic Spring Boot system that ingests, stores, and retrieves telemetry data from optical network elements.

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    OptiNet Phase 1                          │
│              (Monolithic Synchronous System)                │
└─────────────────────────────────────────────────────────────┘

    ┌────────────────────┐
    │   Network Elements │
    │   (NE Simulators)  │
    │                    │
    │  - OTN Transponder │
    │  - DWDM Amplifier  │
    │  - Cross-Connect   │
    │  - Optical Switch  │
    └────────┬───────────┘
             │ HTTPS
             ▼
    ┌────────────────────────┐
    │  Spring Boot REST API  │
    │  (TelemetryController) │
    └────────┬───────────────┘
             │
             ▼
    ┌────────────────────────────────┐
    │  Domain Logic                  │
    │  (TelemetryService)            │
    │  (NeService)                   │
    │  (AlarmService)                │
    └────────┬───────────────────────┘
             │
             ▼
    ┌────────────────────────────────┐
    │  PostgreSQL Persistence Layer  │
    │  (JPA Repositories)            │
    │                                │
    │  Tables:                       │
    │  - network_elements            │
    │  - kpi_metrics                 │
    │  - alarms                      │
    │  - sla_records                 │
    └────────────────────────────────┘
```

### Key Observations

1. **Synchronous Request-Response:** NEs send telemetry → API returns 200 OK immediately
2. **Single Deployable:** One JAR file, one database
3. **Direct Persistence:** No message queue yet
4. **Stateless Controllers:** Each request is independent

---

## 2. Domain Model

### Network Element (NE)

A physical or logical device in the optical network that generates telemetry.

```java
// Using Java 17+ records for immutability and clarity

public record NetworkElement(
    String neId,              // Unique identifier (e.g., "OTN-TXP-001")
    String neName,            // Human-readable name
    NeType neType,            // TRANSPONDER, AMPLIFIER, SWITCH, CROSS_CONNECT
    String neModel,           // Equipment model (e.g., "Infinera-XTM")
    String location,          // Physical location (e.g., "NYC-Data-Center-1")
    NeStatus status,          // OPERATIONAL, DEGRADED, FAILED
    long lastHeartbeat        // Unix timestamp of last communication
) {}

public enum NeType {
    TRANSPONDER,      // Sends/receives optical signals
    AMPLIFIER,        // Boosts signal power
    SWITCH,           // Routes between fibers
    CROSS_CONNECT,    // Wavelength cross-connect
    OPTICAL_MODEM     // Digital modulation/demodulation
}

public enum NeStatus {
    OPERATIONAL,      // Healthy
    DEGRADED,         // Functional but with issues
    FAILED,           // Non-functional
    UNKNOWN           // No recent data
}
```

**Why Records?**
- Immutable by design (thread-safe)
- Concise syntax reduces boilerplate
- Modern Java 17+ best practice
- Perfect for DTO/entity mapping

### KPI Metric

Key Performance Indicator—a measured quantity from a network element.

```java
public record KpiMetric(
    String metricId,          // UUID
    String neId,              // Reference to network element
    KpiType kpiType,          // SNR, BER, POWER, TEMPERATURE, etc.
    double value,             // Measured value
    String unit,              // dB, bits/sec, mW, Celsius, etc.
    long timestamp,           // When measured (Unix ms)
    KpiThresholdStatus status // NORMAL, WARNING, CRITICAL
) {}

public enum KpiType {
    // Optical metrics
    OPTICAL_POWER("dBm"),
    SIGNAL_NOISE_RATIO("dB"),
    CHROMATIC_DISPERSION("ps/nm"),
    POLARIZATION_MODE_DISP("ps"),
    
    // Electrical metrics
    BIT_ERROR_RATE("errors/sec"),
    Q_FACTOR("linear"),
    
    // Environmental
    TEMPERATURE("°C"),
    HUMIDITY("% RH"),
    
    // Operational
    LASER_WAVELENGTH("nm"),
    TRANSCEIVER_POWER("mW"),
    LASER_BIAS_CURRENT("mA");
    
    public final String unit;
    KpiType(String unit) { this.unit = unit; }
}

public enum KpiThresholdStatus {
    NORMAL,      // Within acceptable range
    WARNING,     // Approaching threshold
    CRITICAL     // Exceeds threshold—alert required
}
```

**Why Enums?**
- Sealed types prevent invalid values
- Type-safe in queries and filters
- Easy to add new KPI types without breaking code

### Alarm

An event triggered when a KPI exceeds thresholds or NE status changes.

```java
public record Alarm(
    String alarmId,           // UUID
    String neId,              // Affected network element
    AlarmSeverity severity,   // CRITICAL, MAJOR, MINOR, WARNING
    String alarmText,         // Human-readable description
    long occurredAt,          // When the condition started
    Long clearedAt,           // When resolved (null if ongoing)
    AlarmStatus status        // ACTIVE, ACKNOWLEDGED, CLEARED
) {}

public enum AlarmSeverity {
    CRITICAL,     // Service impact
    MAJOR,        // Significant degradation
    MINOR,        // Minor issue
    WARNING       // Informational
}

public enum AlarmStatus {
    ACTIVE,       // Unresolved
    ACKNOWLEDGED, // Seen by operator
    CLEARED       // Resolved
}
```

### SLA Record

Tracks service-level agreement compliance over time windows.

```java
public record SlaRecord(
    String slaId,
    String neId,
    SlaMetric metric,         // AVAILABILITY, LATENCY, BER, etc.
    double targetValue,       // e.g., 99.99% availability
    double achievedValue,     // Measured during window
    long windowStartTime,     // Day/week/month boundary
    long windowEndTime,
    SlaStatus status          // MET, VIOLATED
) {}

public enum SlaMetric {
    AVAILABILITY,
    MEAN_TIME_BETWEEN_FAILURES,
    MEAN_TIME_TO_RECOVERY,
    BIT_ERROR_RATE,
    LATENCY
}

public enum SlaStatus {
    MET,
    VIOLATED
}
```

---

## 3. Data Flow

### Scenario: NE Sends Telemetry

```
1. Network Element (External System)
   └─ Generates KPI measurement every 30 seconds
   └─ Example: SNR = 18.5 dB at 2024-01-15T10:30:45Z

2. HTTP POST /api/v1/telemetry
   └─ Request Body:
      {
        "neId": "OTN-TXP-001",
        "kpiType": "SIGNAL_NOISE_RATIO",
        "value": 18.5,
        "timestamp": 1705318245000
      }

3. Spring Boot (TelemetryController)
   └─ Validates input (no null, value in valid range)
   └─ Calls TelemetryService

4. TelemetryService (Business Logic)
   └─ Check if NE exists; if not, create or reject
   └─ Determine threshold status (is 18.5 dB normal or warning?)
   └─ Create KpiMetric record
   └─ Check if threshold crossed → trigger alarm
   └─ Save to database

5. KpiMetricRepository (JPA)
   └─ INSERT INTO kpi_metrics (ne_id, kpi_type, value, ...)

6. Response
   └─ HTTP 201 Created
   └─ Response Body: { "metricId": "uuid", "status": "NORMAL" }

7. If Alarm Triggered
   └─ Also INSERT INTO alarms
   └─ Future: emit Kafka event (Phase 2)
```

### Scenario: Query NE Health

```
1. HTTP GET /api/v1/network-elements/{neId}

2. TelemetryController
   └─ Calls NeService.getNetworkElement(neId)

3. NeService
   └─ Loads NE from database
   └─ Loads latest KPI metrics (last 10)
   └─ Loads active alarms
   └─ Constructs NeHealthDto with aggregated view

4. Response
   └─ {
       "neId": "OTN-TXP-001",
       "neType": "TRANSPONDER",
       "status": "OPERATIONAL",
       "recentMetrics": [
         { "kpiType": "SNR", "value": 18.5, "status": "NORMAL" },
         { "kpiType": "POWER", "value": -8.2, "status": "NORMAL" }
       ],
       "activeAlarms": []
     }
```

---

## 4. Technology Choices & Rationale

### Why PostgreSQL (Not In-Memory or NoSQL)?

| Concern | Decision | Reason |
|---------|----------|--------|
| Consistency | PostgreSQL ACID | Alarms, SLA records must be reliable; no data loss |
| Indexing | B-tree on (neId, timestamp) | Fast queries for "latest 10 metrics for NE" |
| Scalability | Partitioning by date | Metrics table grows fast; partition on timestamp |
| Analytics | JSON columns | Can store structured metric metadata |

**Trade-off:** Synchronous writes are slower than async queues, but Phase 1 prioritizes correctness.

### Why Spring Boot (Not Micronaut or Quarkus)?

| Aspect | Spring Boot | Alternative |
|--------|-------------|------------|
| Ecosystem | Massive (Spring Data, Spring Cloud) | Smaller but nimble |
| Team familiarity | Industry standard | Less common |
| Observability | Spring Boot Actuator built-in | Requires manual setup |
| Learning curve | Gentle with good docs | Steeper for beginners |

**For this project:** Spring Boot's ecosystem matters more as we add Kafka and Pinot later.

### Why Gradle (Not Maven)?

| Feature | Gradle | Maven |
|---------|--------|-------|
| Build script | Kotlin DSL (clean) | XML (verbose) |
| Performance | Incremental compilation | Slower |
| Multi-module | Natural | Requires careful pom.xml |
| Task definition | Flexible | Limited |

**Decision:** Gradle's Kotlin DSL makes custom tasks for database migrations and Docker integration easier.

### Why Java 17+ Records?

```java
// Pre-17: Boilerplate
class KpiMetric {
    private String metricId;
    private String neId;
    private KpiType kpiType;
    private double value;
    
    // 30 lines of getters, setters, equals(), hashCode(), toString()
}

// Post-17: Clean
record KpiMetric(
    String metricId,
    String neId,
    KpiType kpiType,
    double value
) {}
```

Benefits:
- Immutable by default (thread-safe)
- Auto-generates `equals()`, `hashCode()`, `toString()`
- Great for DTOs and entity mapping
- Reduced cognitive load

---

## 5. Project Structure

```
src/main/java/com/optinet/
├── OptiNetApplication.java          # Spring Boot entry point
│
├── domain/
│   ├── model/
│   │   ├── NetworkElement.java      # Record
│   │   ├── KpiMetric.java           # Record
│   │   ├── Alarm.java               # Record
│   │   ├── SlaRecord.java           # Record
│   │   └── enums/
│   │       ├── NeType.java
│   │       ├── NeStatus.java
│   │       ├── KpiType.java
│   │       ├── AlarmSeverity.java
│   │       └── SlaMetric.java
│   │
│   ├── repository/
│   │   ├── NetworkElementRepository.java
│   │   ├── KpiMetricRepository.java
│   │   ├── AlarmRepository.java
│   │   └── SlaRecordRepository.java
│   │
│   └── service/
│       ├── TelemetryService.java    # Core business logic
│       ├── NeService.java           # NE management
│       ├── AlarmService.java        # Alarm generation/management
│       └── SlaService.java          # SLA tracking
│
├── api/
│   ├── controller/
│   │   ├── TelemetryController.java # POST /api/v1/telemetry
│   │   ├── NeController.java        # GET /api/v1/network-elements/{neId}
│   │   └── AlarmController.java     # GET /api/v1/alarms
│   │
│   ├── dto/
│   │   ├── TelemetryRequest.java    # @RequestBody for telemetry ingestion
│   │   ├── KpiMetricResponse.java   # API response
│   │   ├── NeHealthDto.java         # Aggregated NE view
│   │   └── AlarmDto.java
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── NeNotFoundException.java
│       └── InvalidTelemetryException.java
│
├── infrastructure/
│   ├── config/
│   │   ├── WebConfig.java           # CORS, interceptors
│   │   └── JpaConfig.java           # Entity scanning, naming strategy
│   │
│   └── persistence/
│       ├── entity/                  # JPA @Entity versions
│       │   ├── NetworkElementEntity.java
│       │   ├── KpiMetricEntity.java
│       │   └── AlarmEntity.java
│       │
│       └── converter/
│           └── KpiTypeConverter.java # JPA converters for enums
│
└── util/
    ├── KpiThresholdEvaluator.java   # Logic to determine NORMAL/WARNING/CRITICAL
    └── TimeUtil.java                # Timestamp handling
```

### Key Decisions

**Records vs. JPA Entities:**
- **Records** for DTOs and domain models (immutable, API-friendly)
- **@Entity classes** for JPA persistence (mutable, compatible with Hibernate)
- **Converter layer** between them in service

Example:
```java
// Domain model (immutable)
record KpiMetric(...) {}

// JPA entity (mutable, with @Id, @Column, etc.)
@Entity
class KpiMetricEntity {
    @Id private String metricId;
    @Column private String neId;
    // ... mappings
}

// Service layer converts
public KpiMetric saveMetric(KpiMetric metric) {
    KpiMetricEntity entity = new KpiMetricEntity(metric);
    repository.save(entity);
    return entity.toDomain();
}
```

---

## 6. Database Schema (PostgreSQL)

### Table: network_elements

```sql
CREATE TABLE network_elements (
    ne_id VARCHAR(100) PRIMARY KEY,
    ne_name VARCHAR(255) NOT NULL,
    ne_type VARCHAR(50) NOT NULL,  -- TRANSPONDER, AMPLIFIER, etc.
    ne_model VARCHAR(100),
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL,   -- OPERATIONAL, DEGRADED, FAILED
    last_heartbeat BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ne_type ON network_elements(ne_type);
CREATE INDEX idx_ne_status ON network_elements(status);
```

### Table: kpi_metrics

```sql
CREATE TABLE kpi_metrics (
    metric_id UUID PRIMARY KEY,
    ne_id VARCHAR(100) NOT NULL REFERENCES network_elements(ne_id),
    kpi_type VARCHAR(100) NOT NULL,  -- SNR, BER, POWER, etc.
    value NUMERIC(12, 4) NOT NULL,
    unit VARCHAR(50),
    threshold_status VARCHAR(50) NOT NULL,  -- NORMAL, WARNING, CRITICAL
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Critical index: lookup recent metrics for a specific NE
CREATE INDEX idx_kpi_ne_timestamp ON kpi_metrics(ne_id, timestamp DESC);
CREATE INDEX idx_kpi_type ON kpi_metrics(kpi_type);
CREATE INDEX idx_kpi_timestamp ON kpi_metrics(timestamp DESC);
```

**Indexing Strategy:**
- `(ne_id, timestamp DESC)`: Fast retrieval of "latest 10 metrics for NE-001"
- Separate index on `kpi_type`: Queries like "all SNR metrics"
- `timestamp DESC`: Time-series queries typically want newest first

### Table: alarms

```sql
CREATE TABLE alarms (
    alarm_id UUID PRIMARY KEY,
    ne_id VARCHAR(100) NOT NULL REFERENCES network_elements(ne_id),
    severity VARCHAR(50) NOT NULL,  -- CRITICAL, MAJOR, MINOR, WARNING
    alarm_text TEXT NOT NULL,
    occurred_at BIGINT NOT NULL,
    cleared_at BIGINT,
    status VARCHAR(50) NOT NULL,    -- ACTIVE, ACKNOWLEDGED, CLEARED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alarm_ne_status ON alarms(ne_id, status);
CREATE INDEX idx_alarm_severity ON alarms(severity);
CREATE INDEX idx_alarm_timestamp ON alarms(occurred_at DESC);
```

### Table: sla_records

```sql
CREATE TABLE sla_records (
    sla_id UUID PRIMARY KEY,
    ne_id VARCHAR(100) NOT NULL REFERENCES network_elements(ne_id),
    metric VARCHAR(100) NOT NULL,   -- AVAILABILITY, LATENCY, etc.
    target_value NUMERIC(10, 4) NOT NULL,
    achieved_value NUMERIC(10, 4) NOT NULL,
    window_start_time BIGINT NOT NULL,
    window_end_time BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,    -- MET, VIOLATED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sla_ne_window ON sla_records(ne_id, window_start_time DESC);
```

**Partitioning Strategy (Optional but Recommended):**
```sql
-- For production, partition kpi_metrics by date
CREATE TABLE kpi_metrics_2024_01 PARTITION OF kpi_metrics
    FOR VALUES FROM ('2024-01-01'::timestamp) TO ('2024-02-01'::timestamp);
```

---

## 7. API Specifications (Phase 1)

### POST /api/v1/telemetry

**Ingest a KPI metric from a network element.**

Request:
```json
{
  "neId": "OTN-TXP-001",
  "kpiType": "SIGNAL_NOISE_RATIO",
  "value": 18.5,
  "timestamp": 1705318245000,
  "unit": "dB"
}
```

Response (201 Created):
```json
{
  "metricId": "550e8400-e29b-41d4-a716-446655440000",
  "neId": "OTN-TXP-001",
  "kpiType": "SIGNAL_NOISE_RATIO",
  "value": 18.5,
  "thresholdStatus": "NORMAL",
  "timestamp": 1705318245000
}
```

**Validation Rules:**
- `neId` must exist (check in database; reject unknown NEs)
- `value` must be numeric and within kpiType's valid range
- `timestamp` must not be in the future
- `kpiType` must be a known enum value

---

### GET /api/v1/network-elements/{neId}

**Retrieve the current health and status of a network element.**

Response (200 OK):
```json
{
  "neId": "OTN-TXP-001",
  "neName": "Transponder Unit 1",
  "neType": "TRANSPONDER",
  "neModel": "Infinera-XTM",
  "location": "NYC-Data-Center-1",
  "status": "OPERATIONAL",
  "lastHeartbeat": 1705318245000,
  "recentMetrics": [
    {
      "metricId": "550e8400-e29b-41d4-a716-446655440000",
      "kpiType": "SIGNAL_NOISE_RATIO",
      "value": 18.5,
      "unit": "dB",
      "thresholdStatus": "NORMAL",
      "timestamp": 1705318245000
    },
    {
      "metricId": "550e8400-e29b-41d4-a716-446655440001",
      "kpiType": "BIT_ERROR_RATE",
      "value": 1.2e-10,
      "unit": "errors/sec",
      "thresholdStatus": "NORMAL",
      "timestamp": 1705318245000
    }
  ],
  "activeAlarms": []
}
```

---

### GET /api/v1/alarms?severity=CRITICAL&status=ACTIVE

**Query alarms with optional filtering.**

Response (200 OK):
```json
{
  "totalCount": 2,
  "alarms": [
    {
      "alarmId": "550e8400-e29b-41d4-a716-446655440002",
      "neId": "OTN-AMP-001",
      "severity": "CRITICAL",
      "alarmText": "Signal-to-noise ratio fell below 15 dB on amplifier OTN-AMP-001",
      "occurredAt": 1705318200000,
      "clearedAt": null,
      "status": "ACTIVE"
    }
  ]
}
```

---

### POST /api/v1/network-elements

**Register a new network element (setup/provisioning).**

Request:
```json
{
  "neId": "OTN-TXP-002",
  "neName": "Transponder Unit 2",
  "neType": "TRANSPONDER",
  "neModel": "Infinera-XTM",
  "location": "NYC-Data-Center-1"
}
```

Response (201 Created):
```json
{
  "neId": "OTN-TXP-002",
  "neName": "Transponder Unit 2",
  "neType": "TRANSPONDER",
  "status": "OPERATIONAL",
  "createdAt": 1705318245000
}
```

---

## 8. Exception Handling Strategy

**Spring global exception handler** for clean, consistent error responses:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNeNotFound(
        NeNotFoundException ex) {
        return ResponseEntity
            .status(NOT_FOUND)
            .body(new ErrorResponse(
                "NE_NOT_FOUND",
                ex.getMessage(),
                System.currentTimeMillis()
            ));
    }
    
    @ExceptionHandler(InvalidTelemetryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTelemetry(
        InvalidTelemetryException ex) {
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(new ErrorResponse(
                "INVALID_TELEMETRY",
                ex.getMessage(),
                System.currentTimeMillis()
            ));
    }
}
```

**Response Format:**
```json
{
  "errorCode": "NE_NOT_FOUND",
  "message": "Network element OTN-TXP-001 not found",
  "timestamp": 1705318245000
}
```

---

## 9. Key Design Decisions & Rationale

### Decision 1: Synchronous Direct Persistence (Phase 1)

**Choice:** Every telemetry POST → write to PostgreSQL immediately

**Pros:**
- Simple, easy to understand
- Guaranteed consistency
- No need to manage a queue
- Easy to debug and test

**Cons:**
- Database becomes bottleneck at high scale
- If DB is slow, API responses slow down
- No replay capability
- Tight coupling between NE and database

**Why for Phase 1:** Baseline correctness. Phase 2 introduces Kafka for decoupling.

---

### Decision 2: Records for Domain Models

**Choice:** Use Java records instead of traditional classes

**Why:**
- Immutability reduces concurrency bugs
- Less boilerplate (no getters/setters)
- Thread-safe by design
- Modern Java best practice (17+)

**Caveat:** Records are final and cannot be JPA entities directly. Use adapter pattern to convert between records and mutable JPA entities.

---

### Decision 3: Enum-Driven KPI Type System

**Choice:** Pre-defined enum for KPI types, not arbitrary strings

**Why:**
- Prevents typos and invalid KPI names
- Type-safe in code and queries
- Easy to add thresholds specific to each KPI type
- Simplifies analytics (group by KPI type)

**Alternative (Rejected):** Allow arbitrary string kpi_type
- Problem: "SNR" vs "snr" vs "signal_noise_ratio" = data inconsistency

---

### Decision 4: Timestamp as Unix Milliseconds

**Choice:** Store timestamps as BIGINT (Unix ms) not TIMESTAMP

**Why:**
- Enables precise time-series queries
- Avoids timezone confusion
- Efficient indexing and sorting
- Standard in time-series systems (Pinot, InfluxDB)

**Conversion:** Spring can serialize Java's `Instant` to/from Unix ms automatically.

---

### Decision 5: PostgreSQL Over NoSQL (MongoDB, Cassandra)

**Trade-off Analysis:**

| Requirement | PostgreSQL | Cassandra | MongoDB |
|---|---|---|---|
| Transactional consistency | ✅ ACID | ⚠️ Eventual | ⚠️ Document-level |
| SLA enforcement (no data loss) | ✅ Yes | ⚠️ Complex | ⚠️ Complex |
| Structured relational schema | ✅ Natural | ❌ Not ideal | ❌ Not ideal |
| Horizontal scale (millions/sec) | ⚠️ Vertical scale | ✅ Yes | ⚠️ Shard management |
| Complex queries (SLA over weeks) | ✅ SQL | ❌ Limited | ⚠️ Aggregation pipeline |
| Observability (explain plans) | ✅ Excellent | ⚠️ Limited | ⚠️ Limited |

**For Phase 1:** PostgreSQL's ACID guarantees and rich query language matter. We'll move *analytics* to Pinot in Phase 3 while keeping relational data in PostgreSQL.

---

## 10. Testing Strategy (Overview)

### Unit Tests
- Test threshold evaluation logic in `KpiThresholdEvaluator`
- Test SLA computation logic
- No database, no Spring context

### Integration Tests
- Test `TelemetryService` with in-memory H2 database
- Verify alarm generation on threshold breach
- Verify JPA repository queries

### Contract Tests (Future)
- Document API contracts
- Validate NE simulators conform to expected request format

---

## 11. Deployment & Local Development Setup

### Docker Compose (Development)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: optinet
      POSTGRES_USER: optinet
      POSTGRES_PASSWORD: optinet123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  optinet-api:
    build:
      context: .
      dockerfile: docker/Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/optinet
      SPRING_DATASOURCE_USERNAME: optinet
      SPRING_DATASOURCE_PASSWORD: optinet123
    ports:
      - "8080:8080"
    depends_on:
      - postgres

volumes:
  postgres-data:
```

**Run locally:**
```bash
docker-compose -f docker/docker-compose.yml up
```

---

## 12. Configuration (application.yml)

```yaml
spring:
  application:
    name: optinet-api
  
  datasource:
    url: jdbc:postgresql://localhost:5432/optinet
    username: optinet
    password: optinet123
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create; use Flyway
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 50
          fetch_size: 50
  
  h2:
    console:
      enabled: true

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.optinet: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

---

## 13. What You'll Learn in Phase 1

### Java 17+ Concepts
- ✅ Records (immutability, pattern matching)
- ✅ Sealed classes (type hierarchy control)
- ✅ Text blocks (multi-line strings)
- ✅ Stream API (functional transformations)
- ✅ Lambda expressions and method references

### Spring Boot
- ✅ REST controllers and request mapping
- ✅ Dependency injection and component lifecycle
- ✅ Spring Data JPA repositories
- ✅ Transaction management (@Transactional)
- ✅ Global exception handling
- ✅ Configuration properties (application.yml)
- ✅ Spring Boot Actuator basics

### PostgreSQL & Databases
- ✅ Schema design (normalized tables, FKs)
- ✅ Indexing strategies (composite indexes)
- ✅ ACID guarantees and transactions
- ✅ Data types (UUID, NUMERIC, BIGINT, ENUM)
- ✅ Query optimization (EXPLAIN ANALYZE)
- ✅ Connection pooling (HikariCP)

### Software Architecture
- ✅ Layered architecture (controller → service → repository)
- ✅ Separation of concerns (domain model vs. JPA entity vs. DTO)
- ✅ Adapter pattern (record ↔ entity conversion)
- ✅ Exception handling strategies
- ✅ Configuration management

### Common Pitfalls Avoided
1. **Mixing domains:** Don't use JPA entities as DTOs
2. **Inconsistent timestamps:** Use Unix ms consistently
3. **No validation:** Always validate input in controllers
4. **Tight coupling:** Use services, not repositories directly in controllers
5. **Silent failures:** Log errors, use custom exceptions

---

## 14. Next Steps (Phase 2 Preview)

Once Phase 1 is stable:
1. Introduce **Apache Kafka** as a message queue
2. Decouple NE → API → Database with events
3. Learn producer/consumer patterns
4. Implement asynchronous telemetry processing
5. Add message replay capability

---

## 15. Success Criteria for Phase 1

- ✅ All APIs working end-to-end
- ✅ PostgreSQL schema clean and performant
- ✅ Unit & integration tests passing (>80% coverage)
- ✅ Docker Compose environment reproducible
- ✅ Code follows naming conventions (Java/Spring standards)
- ✅ Documentation complete and clear
- ✅ Ready to add Kafka without refactoring domain models

---

**Phase 1 is about building a solid foundation with proven patterns. Don't optimize prematurely; clarity over cleverness.**
