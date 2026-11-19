# Static analysis

## Tools and rule sets
- **SpotBugs 4.8.6.6** (spotbugs-maven-plugin) with the **find-sec-bugs 1.13.0** plugin.
  Effort `Max`, threshold `Medium`. All default bug patterns, none disabled.
- **PMD** via maven-pmd-plugin 3.25.0, using `pmd-ruleset.xml`: the `errorprone`,
  `multithreading` and `security` categories. Style and documentation categories are left out
  on purpose, because Checkstyle covers them.

## Reproduce
```bash
mvn -DskipTests compile spotbugs:spotbugs pmd:pmd
# outputs: target/spotbugsXml.xml, target/pmd.xml
```

## Before (iteration-1 code, tag v1.0-iter1)
Reports: `before/spotbugs.xml`, `before/pmd.xml`.

SpotBugs, 11 findings (also 11 at threshold Low), all MALICIOUS_CODE:
- `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`: classes return or store mutable collections or dates
  directly. Affected: `DetectConflictsResponse`, `FinalizeDecisionResponse`,
  `SubmitEvidenceRequest`, `Evidence`, `FinalDecision`, `EvidenceService`.

PMD, 8 findings:
- `AvoidSynchronizedAtMethodLevel` in `ConsensusService`, `EvidenceService` (real: coarse locking).
- `UseConcurrentHashMap` in `ConsensusService`.
- `AvoidFieldNameMatchingMethodName`, `AvoidFieldNameMatchingTypeName`, `AvoidLiteralsInIfCondition`
  (mostly readability).

find-sec-bugs reported no security findings.

## AI-assisted logic review (third technique)
SpotBugs and PMD do not find the logic bugs from the iteration-1 feedback
(`minimumEvidenceCount = 0`, `requestId` reuse). An AI-assisted review does:
[before/ai-review.md](before/ai-review.md) lists 13 findings (4 high, 5 medium, 4 low), 7 of
them reproduced against the running service, with file and line, reproduction and fix.

## After
W1 fixes the findings above, reruns both tools and commits the clean output to `after/`.
