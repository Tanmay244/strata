# Team and workstreams

Iteration-1 ownership is taken from commit authors, PR authors and issue assignees
(M1 #1 Tushar, M2 #2 heysiridu, M3 #3 Shun Yan, M4 #4 Owin Gong, M5 #5 Dev Bahl,
M6 #6 Tanmay for integration, docs, project management and AI documentation).

| Workstream | Owner | GitHub | Iteration-1 work | Unit-test partition duty | Review goes to |
| --- | --- | --- | --- | --- | --- |
| W1 Repo, CI, quality gates | Tanmay Agrawal | Tanmay244 | M6 integration, docs, merges | Cross-cutting checks | W2 |
| W2 Persistence | Tushar Mittal | tush7301 | M1 store, models, idempotency helper | Store, models, idempotency | W3 |
| W3 Client identity, bug fixes | Siri Du | heysiridu | M2 Create, Finalize | Create Analysis, Finalize Decision | W4 |
| W4 Client, end-to-end tests | Owin Gong | littlefishy1 | M4 Detect Conflicts | Detect Conflicts (add the missing tests) | W5 |
| W5 Cloud, documentation | Dev Bahl | devbahl12 | M5 Compute Consensus | Compute Consensus | W6 |
| W6 Test lead | Shun Yan | yanshun1111 | M3 Submit Evidence, 28-request Postman suite | Submit Evidence | W1 |

Review ring: W1 -> W2 -> W3 -> W4 -> W5 -> W6 -> W1. A PR is reviewed and merged by the next
person in the ring, never by its author. Reviewers rotate off the ring when the next owner is
the author's own area expert or is unavailable.

Task board: see the Project management section of the README.
