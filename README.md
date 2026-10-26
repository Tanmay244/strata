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

Detect Conflicts (M4) is implemented. The other operations return `501 Not Implemented`
until their owning milestones land.

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

Run `mvn verify` for Checkstyle, Mockito service tests, Spring MockMvc API tests, and the
existing skeleton tests. M4 tests cover scoring, thresholds, rejected/same-source evidence,
missing analyses, insufficient evidence, response ordering, repeatability, and no mutation.

## M4 AI assistance disclosure

OpenAI Codex generated the M4 implementation, tests, Postman assets, and associated
documentation in response to the request to implement Issue 4 using the supplied proposal
and milestone requirements. See [M4 AI notes](docs/M4-AI-NOTES.md) for scope and prompt notes.
