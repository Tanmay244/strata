# M4 AI assistance

- Tool: OpenAI Codex, used for local code authoring and verification.
- User prompt: implement Issue 4 / M4 Detect Conflicts using the supplied milestone list,
  project proposal V2.0, and advice to keep the service general and avoid direct LLM APIs.
- Context inspected: proposal Operation 3, the existing M1 models/store/controllers,
  Maven configuration, README, and existing tests.
- AI-generated/edited scope: `ConflictService`, `ConflictController`,
  `DetectConflictsResponse`, `ConflictServiceTest`, `ConflictControllerTest`,
  `ConflictFixtures`, `ConflictApiTestApplication`, `postman/` assets, and M4 documentation.
- Design choice: compare normalized categorical claims and confidence gaps with the
  deterministic scoring formula documented in README. The proposal does not prescribe
  an exact scoring formula or insufficient-evidence HTTP status; M4 uses HTTP 409.
- Token count: unavailable in this session's tool outputs; no count has been estimated
  or invented. Add the exported session usage if required for the final team submission.
- Third-party source code: no third-party implementation was copied into these changes.
  Tests use the existing Spring Boot/JUnit/Mockito dependencies; API testing uses Newman.

The service implementation makes no calls to OpenAI, Claude, or any other LLM API.
