# Contributing

## Rules for `main`
- No direct commits. Every change goes through a pull request.
- Every PR needs one approval from a teammate who is not the author, and the CI checks must pass.
- The reviewer merges (squash). The author never merges their own PR.
- Reviewers must read the diff and leave at least one real comment. Rubber-stamp approvals do not count.

## Review rotation
Authors request review from the next person in the ring: W1 -> W2 -> W3 -> W4 -> W5 -> W6 -> W1.
Fill in the names in `docs/team.md`.

## Commit and PR titles
Say what was added, removed or changed, for example
`Reject minimumEvidenceCount of 0 in Create Analysis`.
Not acceptable: `fix`, `updates`, `wip`.

## Branch names
`feature/<workstream>-<topic>`, `fix/<topic>`, `docs/<topic>`, `chore/<topic>`.

## Before opening a PR
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
mvn clean verify
```

## AI-generated code
Mark any file or method written or debugged by AI, including tests, with a comment such as
`// AI-generated (Claude Code)`. Add `Co-Authored-By` to commits. Log real token counts per
session in `docs/ai-usage/<your-name>.md`. Never copy another person's numbers.
