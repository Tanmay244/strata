# Equivalence partitions: <unit or endpoint>

Copy this file per unit (`docs/partitions/units/<Class>.md`) or per endpoint
(`docs/partitions/api/<endpoint>.md`). Every test should name its partition id in `@DisplayName`,
for example `@DisplayName("P3: minimumEvidenceCount at lower boundary (1)")`.

| Input | Id | Partition | Valid? | Representative / boundary values | Test(s) |
| --- | --- | --- | --- | --- | --- |
| minimumEvidenceCount | P1 | below minimum | invalid | -1, 0 | `createAnalysis_rejectsZero` |
| minimumEvidenceCount | P2 | at minimum | valid | 1 | |
| minimumEvidenceCount | P3 | above minimum | valid | 2, large | |
| minimumEvidenceCount | P4 | omitted | valid | null (default 1) | |
