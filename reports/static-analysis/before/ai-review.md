# AI-assisted logic review: "before" report

- **Target:** `src/main/java` at tag `v1.0-iter1` (main at `90b5e81`). Service code only.
- **Technique:** manual-style review by an AI assistant (Claude Code, Sonnet 5.5). The assistant
  read every service, controller, DTO-validation and exception class and looked for logic bugs,
  not style. No code was changed.
- **Prompt (summary):** "Review the service code for logic bugs: input validation, idempotency,
  state transitions, concurrency, error handling, and mismatches with the README. Cite file and
  line. Do not report style."
- **Verification:** findings marked *reproduced* were run against the built jar
  (`java -jar target/strata-0.0.1-SNAPSHOT.jar --server.port=8099`) with the curl calls shown.
  Findings marked *code reading* were not reproduced at runtime.
- **Token usage:** record the session's real count from `/cost` in `docs/ai-usage/<name>.md`.
- **After:** W1 and the finding owners fix these, rerun this review, and commit the result as
  `reports/static-analysis/after/ai-review.md` showing no remaining findings.

## Summary

| ID | Severity | Finding | Status | Owner |
| --- | --- | --- | --- | --- |
| L1 | High | `minimumEvidenceCount = 0` accepted, analysis can never reach consensus | reproduced | W3 |
| L2 | High | Create Analysis reuses a `requestId` with a different payload and silently returns the old analysis | reproduced | W3 |
| L3 | High | Finalize replay ignores the analysis in the URL and the request body | reproduced | W3 |
| L4 | High | Unanimous low-confidence evidence yields consensus confidence 1.0 | reproduced | W3 |
| L5 | Medium | Evidence, consensus and finalize use separate locks, so they race | code reading | W2 |
| L6 | Medium | Consensus on a FINALIZED analysis returns 200, README says 409 | reproduced | W3 |
| L7 | Medium | Three different error body formats across endpoints | reproduced | W3 |
| L8 | Medium | No upper bound on `minimumEvidenceCount`; no length limits on strings | code reading | W3 |
| L9 | Low | Evidence IDs sort as text, so `ev-10` sorts before `ev-2` | code reading | W3 |
| L10 | Low | `conflictThreshold` accepts Java number suffixes such as `0.5d` | code reading | W3 |
| L11 | Low | Idempotency maps are never evicted; `findEvidenceByAnalysis` scans all evidence | code reading | W2 |
| L12 | Medium | `requestId` is global, so two clients collide and one sees the other's record | code reading | W3 |
| L13 | Low | README consensus example shows 0.8; the code returns 1.0 | reproduced | W5 |

## Findings

### L1. Zero minimum evidence count (reproduced)
`AnalysisService.createAnalysis` rejects only `requested < 0` (`AnalysisService.java`, validation
block). `ConsensusService.validateEvidenceCount` (line 71) later rejects counts below 1, so an
analysis created with 0 is stuck.
```
POST /analyses {"requestId":"a0","topic":"t","minimumEvidenceCount":0}   -> 201 ana-1
POST /analyses/ana-1/evidence ...                                        -> 200
POST /analyses/ana-1/consensus                                           -> 409
```
Fix: reject values below 1 with 400. Partitions: below (-1, 0), at (1), above (2), omitted.

### L2. Same requestId, different payload (reproduced)
`IdempotencyHelper.getOrCreate` is keyed by `requestId` only (`IdempotencyHelper.java:28`).
```
POST /analyses {"requestId":"r1","topic":"A","minimumEvidenceCount":2}                  -> 201 ana-3
POST /analyses {"requestId":"r1","topic":"COMPLETELY DIFFERENT","minimumEvidenceCount":5} -> 200 ana-3
```
The second analysis is never created, yet the client sees success. Fix: store a fingerprint of the
request and return 409 when a known `requestId` arrives with different fields (Submit Evidence
already does this).

### L3. Finalize replay ignores analysis and body (reproduced)
`DecisionService.finalizeDecision` answers any known `requestId` before looking at the path
analysis (`DecisionService.java:58-61`).
```
POST /analyses/ana-4/decision {"requestId":"d1","consensusResultId":"con-6"} -> 200 dec-10
POST /analyses/ana-7/decision {"requestId":"d1","consensusResultId":"con-9"} -> 200 dec-10 (ana-4's decision)
```
`ana-7` stays unfinalized while the client is told it was finalized. Fix: on replay, require the
stored decision's analysis id and consensus id to match, else 409.

### L4. Unanimity ignores confidence (reproduced)
`ConsensusService.selectConsensus` returns `winner.weight() / totalWeight` (line 101). With one
source at confidence 0.1 the result is 1.0.
```
POST evidence {"claim":"APPROVE","confidenceScore":0.1} ; POST consensus -> consensusConfidenceScore 1.0
```
So `minimumConfidenceThreshold` in Finalize can never reject a unanimous low-confidence result.
Fix: define the score so it reflects evidence confidence (for example the share multiplied by
the mean confidence of the winning group) and document it.

### L5. Separate locks race (code reading)
`EvidenceService.submitEvidence` and `ConsensusService.computeConsensus` are each `synchronized`
on their own service instance (`EvidenceService.java:37`, `ConsensusService.java:31`);
`DecisionService.finalizeDecision` has no lock. Evidence can pass the `OPEN` check, then consensus
marks the analysis `READY`, then the evidence is saved, so it is accepted after consensus.
Two finalizes with different requestIds can both pass the FINALIZED check and create two
decisions. Fix: one atomic state transition per analysis (database row lock or
`UPDATE ... WHERE status = 'OPEN'`) in the persistence layer.

### L6. Consensus on FINALIZED returns 200 (reproduced)
`computeConsensus` returns the stored result before the FINALIZED check
(`ConsensusService.java:35-41`), and Finalize requires a stored consensus, so the 409 branch is
unreachable.
```
POST /analyses/ana-4/consensus (already finalized) -> 200 con-6
```
The README says this returns 409. Fix the code or the documentation, and test it.

### L7. Inconsistent error formats (reproduced)
- Create / Finalize: `{"error": "..."}`
- Submit Evidence / Consensus: Spring default `{"timestamp","status","error","path"}` with no
  reason message (the reason from `ResponseStatusException` is dropped)
- Detect Conflicts: `application/problem+json`

Customers cannot parse errors uniformly. Fix: one handler and one format for all endpoints.

### L8. Unbounded input (code reading)
`minimumEvidenceCount` accepts `Integer.MAX_VALUE`; `topic`, `description`, `claim`,
`requestId` and `metadata` have no size limit. Fix: add and document maximum lengths and a
maximum count, with boundary tests at, below and above each.

### L9. Text sort of evidence ids (code reading)
`ConflictService.java:58` and `ConsensusService.java:65` sort `evidenceId` as strings. With ten or
more records, pair order and the "first" claim in each pair change unexpectedly. Fix: sort by
submission time and numeric id, or document lexicographic order.

### L10. Number suffixes accepted (code reading)
`ConflictController` uses `Double.valueOf` (line 37), which accepts `0.5d`, `0.5f` and
hexadecimal floats. Fix: validate with a strict decimal pattern.

### L11. Memory growth and scans (code reading)
Idempotency maps in the services hold every request forever, and
`InMemoryStore.findEvidenceByAnalysis` scans all evidence on every call. Both go away with a
database (indexed by analysis id, receipts with a retention rule).

### L12. Global requestId across clients (code reading)
Idempotency keys are shared by every caller, and ids are sequential (`ana-1`, `ana-2`), so one
client can guess and read or finalize another client's analysis. Fix: scope `requestId` and
records by client id (W3).

### L13. README example (reproduced)
The walkthrough shows `consensusConfidenceScore: 0.8` for two APPROVE records at 0.9 and 0.7.
The code returns 1.0. Resolve together with L4.
