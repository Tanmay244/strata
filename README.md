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
  controller/  one REST controller per operation (some are still stubs)
  service/     one service per operation, holds the business logic
  dto/         request and response bodies per operation
  model/       Analysis, Evidence, ConsensusResult, FinalDecision and status enums
  store/       InMemoryStore, the shared datastore
  common/      IdempotencyHelper, GlobalExceptionHandler, custom HTTP exceptions
```

Configuration locations:

- `src/main/resources/application.properties`: application configuration (default port: 8080).
  Override the port with `SERVER_PORT=8081 mvn spring-boot:run`.
- `pom.xml`: dependencies, Java version, test runner, and Checkstyle plugin configuration.
- `google_checks.xml`: bundled in the Checkstyle dependency; there is no local ruleset file.
- `postman/m3-submit-evidence.postman_collection.json`: M3 API tests and local URL/analysis variables.

The service stores data and idempotency receipts in memory; restarting clears both.

## Endpoints

All five operations are implemented: M2 (Create Analysis, Finalize Decision), M3 (Submit Evidence),
M4 (Detect Conflicts), and M5 (Compute Consensus).

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

## Create Analysis and Finalize Decision (M2)

`POST /analyses` takes `requestId` and `topic` (required) plus `description` and
`minimumEvidenceCount` (optional, default 1). It returns `201 Created`, or `200 OK` with the
original record when the same `requestId` is sent again.

```bash
# Good: create a new analysis
curl -s -X POST http://localhost:8080/analyses \
  -H "Content-Type: application/json" \
  -d '{"requestId":"req-1","topic":"Should we deploy v2.0?","minimumEvidenceCount":2}'
# → 201 {"analysisId":"ana-1","status":"OPEN","createdAt":"...","minimumEvidenceCount":2}

# Good: retry with same requestId → returns original (200, not 201)
curl -s -X POST http://localhost:8080/analyses \
  -H "Content-Type: application/json" \
  -d '{"requestId":"req-1","topic":"Should we deploy v2.0?","minimumEvidenceCount":2}'
# → 200 (same body)

# Bad: missing topic → 400
curl -s -X POST http://localhost:8080/analyses \
  -H "Content-Type: application/json" \
  -d '{"requestId":"req-2"}'

# Bad: negative minimumEvidenceCount → 400
curl -s -X POST http://localhost:8080/analyses \
  -H "Content-Type: application/json" \
  -d '{"requestId":"req-3","topic":"test","minimumEvidenceCount":-1}'
```

`POST /analyses/{analysisId}/decision` takes `requestId` and `consensusResultId` (required) plus
`minimumConfidenceThreshold` (optional, 0 to 1; no check when omitted). The `consensusResultId`
must equal the id of the consensus stored for that analysis. `supportingEvidenceIds` lists only
evidence whose `acceptanceStatus` is `ACCEPTED`. A repeated `requestId` returns the original
decision.

```bash
# Good: finalize after consensus computed (replace IDs with real ones)
curl -s -X POST http://localhost:8080/analyses/ana-1/decision \
  -H "Content-Type: application/json" \
  -d '{"requestId":"dec-1","consensusResultId":"con-1","minimumConfidenceThreshold":0.5}'
# → 200 {"decisionId":"...","finalDecision":"APPROVE","confidenceScore":0.8,...}

# Bad: unknown analysisId → 404
curl -s -X POST http://localhost:8080/analyses/does-not-exist/decision \
  -H "Content-Type: application/json" \
  -d '{"requestId":"dec-2","consensusResultId":"con-1"}'

# Bad: wrong consensusResultId → 422
curl -s -X POST http://localhost:8080/analyses/ana-1/decision \
  -H "Content-Type: application/json" \
  -d '{"requestId":"dec-3","consensusResultId":"wrong-id"}'

# Bad: confidence below threshold → 422
curl -s -X POST http://localhost:8080/analyses/ana-1/decision \
  -H "Content-Type: application/json" \
  -d '{"requestId":"dec-4","consensusResultId":"con-1","minimumConfidenceThreshold":0.99}'
```

Errors are returned as `{"error": "<message>"}`:

| Status | Cause |
|---|---|
| 400 | Missing or invalid field (`requestId`, `topic`, `consensusResultId`, negative `minimumEvidenceCount`, threshold outside 0 to 1) |
| 404 | Analysis does not exist |
| 409 | Analysis is already `FINALIZED` |
| 422 | No consensus for the analysis, `consensusResultId` mismatch, or confidence below the threshold |
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
Use the test fixtures below to exercise M3 in isolation, or run the full service with M2 to create real analyses.

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
With M2 implemented, the same collection can use real analysis IDs via collection/environment
variables `analysisId`, `readyAnalysisId`, and `finalizedAnalysisId`; provision those lifecycle states
before running. The collection itself does not create or close analyses.

M3 AI assistance and third-party code notes: [docs/m3-ai-usage.md](docs/m3-ai-usage.md).

## Detect Conflicts (M4)

`GET /analyses/{analysisId}/conflicts` reads accepted evidence for an existing analysis.
The optional `conflictThreshold` must be finite and in `[0, 1]`; omission uses `0.6`.
An explicitly empty value is invalid. No request body or `requestId` is needed.
At least two **accepted** records are required. OPEN, READY, and FINALIZED analyses can
all be inspected. Rejected records and evidence belonging to other analyses are excluded.

### Conflict rules

The proposal leaves the exact algorithm open. This initial, domain-neutral rule treats
claims within an analysis as alternative categorical recommendations:

1. Sort accepted records by `evidenceId` and compare each unordered pair once.
2. Skip pairs whose `sourceId` values are equal.
3. For comparison only, trim claims, collapse whitespace, and lowercase with `Locale.ROOT`.
   Return the original claims in the response.
4. Let `a` and `b` be the two `normalizedConfidenceScore` values. Claim disagreement is
   `0` for equal normalized claims and `(a + b) / 2` otherwise. The confidence gap is
   `abs(a - b)`. Pair severity is `max(disagreement, confidence gap)`.
5. Return a pair when severity is positive and `>= conflictThreshold`. A zero threshold
   still excludes identical claims with identical confidence.

Scoring uses decimal arithmetic on the stored confidence values before returning a numeric
severity, so values such as `0.7` and `0.6` correctly meet an inclusive threshold of `0.65`.

These rules detect different recommendations and large confidence differences, including
confidence differences for matching claims. They assume M3 supplies valid accepted records
with non-null identifiers/claims and normalized confidence in `[0, 1]`. They do not infer
natural-language meaning: synonyms with different spellings count as different categories.
Clients should use a consistent claim vocabulary within each analysis. The service calls no
LLM APIs. Pairwise comparison costs O(n²), with O(k) space for k reported pairs, plus sorting.

### Response and errors

Each index across the three response arrays describes the same evidence pair. Source and
claim order follows evidence-ID order. Multiple evidence pairs can have the same source pair;
they are not collapsed. No matches returns three empty arrays with HTTP 200.

```json
{
  "conflictingSourcePairs": [["agent-a", "agent-b"]],
  "conflictSeverityScores": [0.8],
  "conflictingClaims": [["APPROVE", "REJECT"]]
}
```

| Status | Meaning |
|---|---|
| `200` | Conflict report, possibly empty |
| `400` | Invalid threshold: empty, nonnumeric, nonfinite, or outside `[0, 1]` |
| `404` | Analysis does not exist |
| `409` | Fewer than two accepted evidence records |

Errors use `application/problem+json` with `type`, `title`, `status`, and `detail`.
Threshold validation precedes analysis lookup. The operation changes no records, statuses,
timestamps, or ID counters, including on failures. Repeating it against unchanged data
returns the same report. Concurrent evidence submissions can affect a subsequent report;
the in-memory datastore does not provide a transactional snapshot across operations.

Using the test-only fixture server described below:

```bash
curl "http://127.0.0.1:8081/analyses/m4-conflicts/conflicts?conflictThreshold=0.8"
# 200: two conflicts, each with severity 0.8
curl "http://127.0.0.1:8081/analyses/m4-conflicts/conflicts?conflictThreshold=2"
# 400: invalid threshold; no state change
```

## API tests and Postman workspace

Import `postman/Strata.postman_collection.json` and `postman/Local.postman_environment.json`
into a Postman workspace named **Strata**. The collection has one numbered folder per
operation, shared URLs, workflow ID capture, and an independent **M4 isolated tests** folder.
See [Postman instructions](postman/README.md) for fixture startup, running tests before M2/M3
merge, and the later five-operation workflow. The fixture launcher is under `src/test` and
is excluded from the production JAR.

Run `mvn verify` for Checkstyle, Mockito service tests, a Spring MockMvc HTTP contract test,
and the existing skeleton tests. The initial-iteration Java suite has 10 tests: 2 for M4
and 8 from the skeleton. M4 retains the default-threshold scoring path, response ordering,
read-only store interactions, and HTTP response serialization.
The controller test mocks the service; Postman separately exercises the live endpoint.

## M4 AI assistance disclosure

OpenAI Codex was used to help the M4 implementation. See [M4 AI notes](docs/M4-AI-NOTES.md) for scope and prompt notes.

## Compute Consensus (M5)

`POST /analyses/{analysisId}/consensus` groups all accepted evidence by normalized claim,
weights each group by the sum of its `confidenceScore` values, and selects the claim with
the highest normalized weight. The analysis status advances from `OPEN` to `READY` and a
`ConsensusResult` record is stored. Re-calling the endpoint when a result already exists
returns the stored result without recomputing.

```bash
# Good: compute consensus after at least minimumEvidenceCount evidence items submitted
curl -s -X POST http://localhost:8080/analyses/ana-1/consensus
# → 200 {"consensusResultId":"con-1","consensusClaim":"APPROVE",
#         "consensusConfidenceScore":0.72,"evidenceCount":3}

# Good: idempotent — calling again returns the same result
curl -s -X POST http://localhost:8080/analyses/ana-1/consensus
# → 200 (same body)

# Bad: analysis does not exist → 404
curl -s -X POST http://localhost:8080/analyses/unknown/consensus

# Bad: not enough evidence yet (below minimumEvidenceCount) → 409
curl -s -X POST http://localhost:8080/analyses/ana-empty/consensus

# Bad: analysis already FINALIZED → 409
curl -s -X POST http://localhost:8080/analyses/ana-done/consensus
```

| Status | Cause |
|---|---|
| 200 | Consensus computed (or existing result returned) |
| 404 | Analysis does not exist |
| 409 | Fewer than `minimumEvidenceCount` accepted evidence records, or analysis is already `FINALIZED`, or evidence has invalid data |

M5 AI assistance notes: [docs/m5-ai-usage.md](docs/m5-ai-usage.md).

## Third-party libraries and tools

All application and test dependencies are fetched by Maven from Maven Central; no source is
copied into this repository.

| Library | Version (via Spring Boot BOM) | Use |
|---|---|---|
| Spring Boot | 3.3.5 | Web framework, dependency injection, test context |
| Spring Web MVC | (BOM) | REST controllers, `ResponseStatusException` |
| Spring Validation | (BOM) | Bean validation support |
| Jackson Databind | (BOM) | JSON serialization / deserialization |
| JUnit 5 | (BOM) | Unit and integration test runner |
| Mockito | (BOM) | Test doubles (mocks) for service-layer tests |
| Checkstyle | 10.17.0 | Style enforcement (Google Java Style) |
| JaCoCo | 0.8.12 | Branch coverage reporting |
| Newman | 6.2.1 (npm, optional) | CLI runner for Postman API tests |

No third-party source code was copied into this repository.

## AI documentation

The team used AI coding assistants across all milestones. Per-milestone records document
the tool, purpose, prompts, and token counts where available.
M3 source files carry inline `// AI-generated` comments. Other milestones are attributed
via commit `Co-Authored-By` tags (M1, M2) or per-milestone notes files.

| Milestone | Tool | Notes |
|---|---|---|
| M1 | Cursor / Claude Sonnet 5.5 | Skeleton, models, store, idempotency helper — see [docs/m1-ai-usage.md](docs/m1-ai-usage.md) and commit co-author tags |
| M2 | Cursor / Claude Sonnet 5.5 | Create Analysis, Finalize Decision, exception hierarchy — see [docs/m2-ai-usage.md](docs/m2-ai-usage.md) |
| M3 | OpenAI Codex | Submit Evidence, Postman collection — see [docs/m3-ai-usage.md](docs/m3-ai-usage.md) |
| M4 | OpenAI Codex | Detect Conflicts, Postman collection — see [docs/M4-AI-NOTES.md](docs/M4-AI-NOTES.md) |
| M5 | Cursor / Claude Sonnet 5.5 | Compute Consensus — see [docs/m5-ai-usage.md](docs/m5-ai-usage.md) |

Token counts per tool session are recorded in the per-milestone notes files where available.

## Project management

Tasks are tracked as GitHub Issues. Each milestone has a corresponding issue that is closed
when the PR merges:
[GitHub Issues — Strata](https://github.com/Tanmay244/strata/issues)

All configuration files needed to build, run, and test the service are in this repository.
No external accounts or credentials are required beyond Java 17 and Maven 3.9+.
