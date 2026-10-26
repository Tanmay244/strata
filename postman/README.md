# Strata Postman collection

Import `Strata.postman_collection.json` and `Local.postman_environment.json` into a workspace
named **Strata**, then select the **Strata - local** environment. This repository supplies
the importable workspace assets; it does not create a hosted Postman workspace.

## Run M4 independently

Create Analysis and Submit Evidence are still stubs. Start the test-only fixture server:

```bash
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.strata.ConflictApiTestApplication
```

In PowerShell, quote the property argument:

```powershell
mvn spring-boot:test-run '-Dspring-boot.run.main-class=com.strata.ConflictApiTestApplication'
```

The launcher uses `127.0.0.1:8081`. It loads fixed records through the in-memory store;
it does not add production seeding/reset endpoints. Test sources and fixtures are excluded
from `mvn package` output. Stop the launcher with Ctrl+C after testing.

Select **M4 isolated tests**, nested under **03 - Detect Conflicts (M4)**, and run the
whole folder in order. Its 20 requests assert the exact report, default/explicit thresholds,
inclusive boundaries, no-conflict results, READY/FINALIZED reads, invalid thresholds,
missing analyses, insufficient accepted evidence, and repeatability after failed requests.
The default request saves a baseline for subsequent assertions. Java tests separately
verify that stored records and ID counters do not change.

These 20 Postman requests remain available independently of the initial-iteration Java
suite. They are not included in the JaCoCo report, which measures Java tests only.

For a command-line run with Node.js/npm:

```bash
npx --yes newman@6.2.1 run postman/Strata.postman_collection.json -e postman/Local.postman_environment.json --folder "M4 isolated tests"
```

| Fixture ID | Data |
|---|---|
| `m4-conflicts` | APPROVE/.9, REJECT/.7, approve/.9 from three sources, plus rejected DEFER |
| `m4-empty` | No evidence |
| `m4-single` | One accepted and one rejected record |
| `m4-compatible` | READY; equivalent claims, equal confidence |
| `m4-finalized` | FINALIZED; opposing claims with confidence 1 |
| `m4-missing` | Intentionally absent |

`fixtureBaseUrl` controls these requests; `baseUrl` controls the normal workflow. They
default to `http://127.0.0.1:8081` and `http://localhost:8080`, respectively. Fixtures are
in-memory and reset when the test server restarts.

## Shared five-operation workflow

The numbered operation folders provide request bodies and basic response assertions for
all five endpoints. The intended order is Create -> Submit (two sources) -> Detect ->
Consensus -> Finalize. Create assigns a new `runId`, and response scripts capture
`analysisId` and `consensusResultId`. Request IDs remain stable for manual retries of a
request within that run.

M2, M3, and M5 must land before this workflow can pass. Their 501 stubs intentionally fail
the success assertions. Run the six workflow requests in numbered order, excluding the
**M4 isolated tests** subfolder. Alternatively, once those milestones are implemented,
start both the normal server and fixture server to run the entire collection.


