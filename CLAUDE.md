# ParcelPlus: context for AI tools
Read docs/architecture.md and docs/conventions.md before changing anything.

## Project
Event-driven parcel tracking. shipment-service (commands, outbox) -> Kafka topic
shipment.events.v1 -> tracking-service (read model, REST + SSE) -> Angular dashboard.

## Hard rules
- All data is invented. No employer or client code, data or domain vocabulary
  (no airline, cargo, airport or baggage terms).
- The owner must be able to explain every line. When you generate code, explain the
  design choice and the trade-off in your reply. Prefer simple code over clever code.
- Small, focused changes. One concern per commit. Do not add dependencies without saying why.
- Contracts first: change contracts/ (OpenAPI, JSON Schema) before the code that uses them.
- Every behavior change comes with a test. Integration tests use Testcontainers.
- Be direct. If a choice is weak or the scope is too big, say so.

## Stack
Java 21, Spring Boot, Maven, PostgreSQL + Flyway, Kafka (KRaft), Angular + RxJS + Tailwind,
Docker, GitHub Actions, AWS ECS Fargate.