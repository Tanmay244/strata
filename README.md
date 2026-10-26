# Strata

Spring Boot service that collects evidence for an analysis, detects conflicts, computes a
consensus, and records a final decision. All data is held in memory.

## Style checker

- **Tool:** [Checkstyle](https://checkstyle.org/) via `maven-checkstyle-plugin`
- **Ruleset:** `google_checks.xml` (Google Java Style), bundled with Checkstyle 10.17.0
- Runs automatically in the `validate` phase and **fails the build on any warning**.
  Run it alone with `mvn checkstyle:check`.

## Build, run, test

Requires Java 17 and Maven 3.9+.

```bash
mvn clean verify          # style check + compile + tests
mvn spring-boot:run       # starts on http://localhost:8080
```

## Project layout

```
src/main/java/com/strata/
  controller/  one REST controller per operation (stubs until implemented)
  service/     one service per operation, holds the business logic
  dto/         request and response bodies per operation
  model/       Analysis, Evidence, ConsensusResult, FinalDecision and status enums
  store/       InMemoryStore, the shared datastore
  common/      IdempotencyHelper, GlobalExceptionHandler
```

Config lives in `src/main/resources/application.properties`; Checkstyle config is in `pom.xml`.

## Endpoints

Stubs return `501 Not Implemented` until the owning milestone lands.

| Operation | Endpoint | Milestone |
|---|---|---|
| Create Analysis | `POST /analyses` | M2 |
| Submit Evidence | `POST /analyses/{analysisId}/evidence` | M3 |
| Detect Conflicts | `GET /analyses/{analysisId}/conflicts?conflictThreshold=` (read-only) | M4 |
| Compute Consensus | `POST /analyses/{analysisId}/consensus` | M5 |
| Finalize Decision | `POST /analyses/{analysisId}/decision` | M2 |

## Idempotency

Operations that create records take a `requestId`. Use one `IdempotencyHelper<T>` per
operation: `getOrCreate(requestId, () -> newRecord)` returns the existing record when the same
`requestId` is sent again.
