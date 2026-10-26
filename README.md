# Strata

Spring Boot service that collects evidence for an analysis, detects conflicts, computes a
consensus, and records a final decision. All data is held in memory.

## Style checker

- **Tool:** [Checkstyle](https://checkstyle.org/) via `maven-checkstyle-plugin`
- **Ruleset:** `google_checks.xml` (Google Java Style), bundled with Checkstyle 10.17.0
- Runs automatically in the `validate` phase and **fails the build on any warning**.
  Run it alone with `mvn checkstyle:check`.

## Build, run, test

Requires Java 17 and Maven 3.9+. Confirm `java -version` and `mvn -version` use Java 17.
On macOS, select it with `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.

```bash
mvn clean verify          # style check + compile + tests
mvn spring-boot:run       # starts on http://localhost:8080
mvn test                 # all JUnit tests
mvn -Dtest=EvidenceServiceTest,EvidenceControllerTest test  # M3 tests only
java -jar target/strata-0.0.1-SNAPSHOT.jar  # after clean verify
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

Configuration locations:

- `src/main/resources/application.properties`: application configuration (default port: 8080).
  Override the port with `SERVER_PORT=8081 mvn spring-boot:run`.
- `pom.xml`: dependencies, Java version, test runner, and Checkstyle plugin configuration.
- `google_checks.xml`: bundled in the Checkstyle dependency; there is no local ruleset file.
- `postman/m3-submit-evidence.postman_collection.json`: M3 API tests and local URL/analysis variables.

The service stores data and idempotency receipts in memory; restarting clears both.

## Endpoints

Submit Evidence (M3) is implemented. Other stubs return `501 Not Implemented` until
their owning milestone lands.

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

## Submit Evidence (M3)

`POST /analyses/{analysisId}/evidence` takes `analysisId` in the path and a JSON body:

```json
{
  "requestId": "evidence-request-1",
  "sourceId": "reviewer-1",
  "claim": "Accept the change",
  "confidenceScore": 0.8,
  "evidenceType": "software-review",
  "metadata": {"checks": ["security", "performance"]}
}
```

`requestId`, `sourceId`, `claim`, `confidenceScore`, and `evidenceType` are required.
Strings must be nonblank; confidence must be a finite number in the inclusive range `[0, 1]`.
`metadata` is an optional JSON object. Evidence types are caller-defined strings.

The service trims surrounding whitespace from strings and collapses internal claim whitespace.
It preserves case and the meaning of claims. Confidence already uses the internal `[0, 1]` scale,
so normalization preserves its value (and canonicalizes negative zero). Missing/null metadata
normalizes to an empty object; provided metadata is copied before storing it.

Success returns `200 OK` for both first submissions and retries:

```json
{
  "evidenceId": "ev-1",
  "normalizedConfidenceScore": 0.8,
  "submittedAt": "2026-10-26T15:00:00Z",
  "acceptanceStatus": "ACCEPTED"
}
```

IDs and timestamps above are illustrative. A new submission reads an existing `OPEN` analysis
and writes one Evidence record. It does not change the analysis status. `READY` and `FINALIZED`
analyses reject new evidence. No rejected Evidence record is written on failure.

A retry with the original `requestId` and equivalent normalized fields returns the exact original
receipt, including its ID and timestamp, even after the analysis closes. Reusing that request ID
with different fields or another analysis returns `409`. Request IDs are scoped to the Submit
Evidence operation across analyses. An exact duplicate under a *new* request ID also returns
`409`: duplicate identity includes analysis, source, claim, evidence type, normalized confidence,
and metadata. The same claim from a different source is allowed. Failed requests do not reserve
request IDs. Concurrent retries and duplicate submissions create at most one evidence record.

| HTTP status | M3 behavior |
|---|---|
| 200 | Evidence accepted or original receipt returned on retry |
| 400 | Missing/blank required field, missing body, malformed JSON, invalid confidence or metadata type |
| 404 | Analysis does not exist |
| 409 | Analysis is closed, incompatible request-ID reuse, or exact duplicate under a new request ID |

Good usage: create an analysis with M2, submit evidence from independent sources while it is
`OPEN`, then compute consensus. Retry network failures with the same request ID and body.
Bad usage: submit to an unknown analysis (`404`), send confidence `1.5` (`400`), send new evidence
after consensus makes the analysis `READY` (`409`), or disguise a duplicate with a new ID (`409`).
M2 is currently a stub on this branch; use the test fixtures below to exercise M3 independently.

## M3 API tests (Postman / Newman)

In a separate terminal, start the test-only launcher from the repository root:

```bash
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.strata.fixture.M3ApiTestApplication
```

Wait for `M3 fixtures ready`. This runs the real HTTP endpoint with fixture analyses `m3-open`,
`m3-ready`, and `m3-finalized`. The launcher lives under `src/test` and is excluded from the
production jar. It adds no fixture/reset HTTP endpoints. Stop it with Ctrl-C when finished.

Import `postman/m3-submit-evidence.postman_collection.json` into Postman and run the entire
collection in order. It contains 28 requests covering success, retries, incompatible reuse,
duplicates, missing/blank fields, confidence boundaries, unknown/closed analyses, and invalid JSON.
The first request generates a fresh run ID, so repeated collection runs do not collide.
M4 can import this operation folder into the shared collection.

Alternatively, run the collection with Newman (requires Node.js/npm):

```bash
npm exec --yes --package=newman@6.2.1 -- newman run postman/m3-submit-evidence.postman_collection.json
```

Collection variables default to `baseUrl=http://localhost:8080` and the fixture analysis IDs.
For another port, append `--env-var baseUrl=http://localhost:8081` to the Newman command and
start the launcher with `-Dspring-boot.run.arguments=--server.port=8081`.
After M2 is implemented, the same collection can use real analysis IDs via collection/environment
variables `analysisId`, `readyAnalysisId`, and `finalizedAnalysisId`; provision those lifecycle states
before running. The collection itself does not create or close analyses.

M3 AI assistance and third-party code notes: [docs/m3-ai-usage.md](docs/m3-ai-usage.md).
