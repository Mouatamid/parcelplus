# Conventions
- Java package root: com.parcelplus.<service>. Events are records; no business logic in them.
- Branches: short-lived feature/<topic>, merged by PR so CI runs. Commits: Conventional Commits
  (feat:, fix:, test:, docs:, chore:).
- Tests: *Test = unit (JUnit 5 + Mockito), *IT = integration (Testcontainers).
- Events: past-tense names (ShipmentCreated), additive schema changes only, bump schemaVersion otherwise.
- Config via environment variables; no secrets in the repo.
- Every non-trivial decision gets an ADR in docs/decisions/ (copy 0000-template.md).