# Strata Postman collection

Import `Strata.postman_collection.json` and `Local.postman_environment.json` into a workspace
named **Strata**, then select the **Strata - local** environment. This repository supplies
the importable workspace assets; it does not create a hosted Postman workspace.

## Five-operation end-to-end workflow

All five operations are implemented. The numbered folders run in sequence:

```
01 - Create Analysis  →  02 - Submit Evidence (×2)  →  03 - Detect Conflicts
  →  04 - Compute Consensus  →  05 - Finalize Decision
```

Start the service:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn spring-boot:run
```

Run the full collection with Newman:

```bash
npx --yes newman@6.2.1 run postman/Strata.postman_collection.json \
  -e postman/Local.postman_environment.json \
  --folder "01 - Create Analysis (M2)" \
  --folder "02 - Submit Evidence (M3)" \
  --folder "03 - Detect Conflicts (M4)" \
  --folder "04 - Compute Consensus (M5)" \
  --folder "05 - Finalize Decision (M2)"
```

Or open Postman, run the five numbered folders in order (exclude **M4 isolated tests**).
The first request in folder 01 generates a fresh `runId` so repeated runs do not collide.
`analysisId` and `consensusResultId` are captured automatically between steps.

## M4 isolated tests

To exercise Detect Conflicts against controlled fixture data, start the fixture server:

```bash
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.strata.ConflictApiTestApplication
```

In PowerShell, quote the property argument:

```powershell
mvn spring-boot:test-run '-Dspring-boot.run.main-class=com.strata.ConflictApiTestApplication'
```

The launcher uses `127.0.0.1:8081`. It loads fixed records through the in-memory store and
does not add production seeding/reset endpoints. Test sources and fixtures are excluded from
`mvn package` output. Stop the launcher with Ctrl+C after testing.

Select **M4 isolated tests**, nested under **03 - Detect Conflicts (M4)**, and run the
whole folder in order. Its 20 requests assert the exact report, default/explicit thresholds,
inclusive boundaries, no-conflict results, READY/FINALIZED reads, invalid thresholds,
missing analyses, insufficient accepted evidence, and repeatability after failed requests.

For a command-line run:

```bash
npx --yes newman@6.2.1 run postman/Strata.postman_collection.json \
  -e postman/Local.postman_environment.json --folder "M4 isolated tests"
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
default to `http://127.0.0.1:8081` and `http://localhost:8080` respectively. Fixtures are
in-memory and reset when the test server restarts.

## M3 dedicated collection

`m3-submit-evidence.postman_collection.json` contains 28 requests for Submit Evidence only.
It uses the M3 fixture launcher (see main README for instructions).

## Collection variables

| Variable | Default | Set by |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | environment file |
| `fixtureBaseUrl` | `http://127.0.0.1:8081` | environment file |
| `runId` | (generated) | prerequest script in folder 01 |
| `analysisId` | (captured) | test script in folder 01 |
| `consensusResultId` | (captured) | test script in folder 04 |
