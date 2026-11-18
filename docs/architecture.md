# ParcelPulse (working name): architecture, decisions and plan
 
As of 2026-11-18. Invented parcel-delivery domain. No airline, cargo, airport or baggage vocabulary anywhere (client overlap). Hubs are `HUB-001` style codes.
 
## 1. Scope in one sentence
Scan events flow from a command service through Kafka into a read-model service, which pushes live updates to an Angular dashboard.
 
## 2. Services (MVP = 2 deployables + web)
| Component | Role | Owns |
|---|---|---|
| shipment-service (Java 21, Spring Boot) | Command side and system of record. REST to create shipments and register status changes. Enforces the status state machine. Writes an outbox row in the same transaction. A poller publishes outbox rows to Kafka. Optional `simulator` profile generates invented traffic. | `shipment_db` |
| tracking-service (Java 21, Spring Boot) | Query side. Idempotent Kafka consumer builds a read model. REST queries plus an SSE stream. | `tracking_db` |
| web/dashboard (Angular, RxJS, Tailwind) | Live shipment list, live feed. Detail timeline and KPI strip come after the MVP. | none |
| notification-service | STRETCH ONLY. Second consumer group raising alerts on EXCEPTION. Probably cut. | none |
 
Honest note: for a one-person system the right production design would be a modular monolith. The split exists to demonstrate event-driven design. Be ready to say that, and to say what the split costs (eventual consistency, two deploys, contracts).
 
## 3. Events and contracts
- One topic, `shipment.events.v1`, key = `shipmentId`, 3 partitions, replication factor 1 (single broker). One topic because Kafka orders only within a partition of one topic, and `ShipmentCreated` must precede `ShipmentStatusChanged` for the same key.
- Dead-letter topic: `shipment.events.v1.dlt`.
- Envelope: `eventId` (UUID), `eventType`, `schemaVersion`, `occurredAt`, `shipmentId`, `sequence` (per shipment, `ShipmentCreated` = 0), plus payload.
- `ShipmentCreated` payload: `trackingCode`, `originHub`, `destinationHub`, `serviceLevel`.
- `ShipmentStatusChanged` payload: `status`, `hubCode`.
- Statuses: CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION. Valid transitions enforced in shipment-service.
- Contracts live in `contracts/`: `openapi/*.yaml` (generated, committed), `events/*.schema.json` (JSON Schema), `examples/*.json`.
- Contract tests: producer validates its serialized events against the schema. Consumer processes every file in `examples/`. A test fails if the generated OpenAPI differs from the committed file. This is schema-contract testing, not consumer-driven Pact. Pact is a stretch.
## 4. Data model
shipment_db (Flyway): `shipment(id, tracking_code unique, origin_hub, destination_hub, service_level, status, version, created_at, updated_at)`, `shipment_status_history(id, shipment_id, sequence, status, hub_code, occurred_at)`, `outbox(id, aggregate_id, event_type, payload jsonb, created_at, published_at null)`.
 
tracking_db (Flyway): `tracking_view(shipment_id pk, tracking_code unique, origin_hub, destination_hub, current_status, last_hub, last_sequence, last_event_at)`, `tracking_timeline(id, shipment_id, sequence, status, hub_code, occurred_at, unique(shipment_id, sequence))`.
 
Consumer rule: `sequence <= last_sequence` is a duplicate and is ignored. `sequence == last_sequence + 1` is applied. A larger sequence is a gap: throw, retry with backoff, then dead-letter.
 
## 5. Key mechanisms (interview material)
- Transactional outbox: avoids the dual-write problem. Poller uses `SELECT ... FOR UPDATE SKIP LOCKED`. Delivery is at-least-once, so the consumer must be idempotent.
- Producer: `enable.idempotence=true`, `acks=all`.
- SSE over WebSocket: one-way, plain HTTP, simple through ALB. Send a heartbeat every ~15 s because ALB idle timeout defaults to 60 s.
- Angular reconnect: fetch snapshot over REST, then resume the stream. RxJS: `EventSource` wrapped in an Observable, `retry` with delay, `scan` as the reducer.
- Java 21 where it earns its place: records and sealed interface for events, pattern-matching switch, virtual threads.
## 6. Decisions
| Decision | Choice | Why | Weakness |
|---|---|---|---|
| Broker | Kafka (KRaft, single broker) | Event log, per-key ordering, replay, consumer groups; fits the domain; matches your stack | More concepts to defend; single broker is demo-grade |
| Cloud | AWS, ECS Fargate | Differentiates from the Azure on your CV, practical proof for CLF-C02 | Azure is cheaper and quicker for this design |
| Kafka on AWS | One small EC2 running Kafka, not MSK | MSK costs hundreds per month | Not production-grade; say so in the README |
| Auth | None. Write endpoints behind an API key in the cloud, dashboard read-only | Scope | Document it |
| Contracts | JSON Schema + OpenAPI drift test | Cheap, explicit | Not consumer-driven |
| Repo | Monorepo | One CI, one README, one clone | Shared pipeline |
 
AWS shape: ALB with path routing to three Fargate services (web nginx, shipment, tracking), RDS PostgreSQL (one small instance, two databases), Kafka on one EC2 instance, ECR, SSM Parameter Store for secrets, public subnets for tasks (avoid NAT gateway cost), GitHub Actions deploying via OIDC. Set a budget alert before creating anything.
 
## 7. Cut from MVP
Notification service, auth, CDC/Debezium, schema registry, Kafka Streams, Kubernetes, tracing, Pact, maps, filters/pagination polish, i18n.
 
## 8. Repo layout
```
services/shipment-service  services/tracking-service  web/dashboard
contracts/{openapi,events,examples}  infra/{compose,terraform}
docs/{architecture.md,decisions/,conventions.md}  .github/workflows  CLAUDE.md
```
 
## 9. Plan (hours: Mon/Wed 1.5, Fri 1, Sat 3)
My estimate for the full MVP list is about 25 h of work against 26.5 h available to Sat 12 Dec, so almost no slack. Mon 14 Dec is the buffer.
 
**W1 (18-21 Nov, 5.5 h)**
- Wed 18: repo, docs skeleton, CLAUDE.md, toolchain check
- Fri 20: compose with Kafka KRaft and Postgres, topic script
- Sat 21: shipment-service skeleton, Flyway, state machine and unit tests, create/get endpoints, first Testcontainers test
**W2 (23-28 Nov, 7 h)**
- Mon 23: status-change endpoint, ProblemDetail errors, OpenAPI committed plus drift test
- Wed 25: outbox table, event records, JSON Schemas, producer schema test
- Fri 27: outbox poller publishing to Kafka
- Sat 28: Testcontainers Kafka test (API to outbox to Kafka record, schema-valid); tracking-service skeleton and consumer happy path
**W3 (30 Nov-5 Dec, 7 h)**
- Mon 30: idempotency/gap rules, DLT, tests
- Wed 2: tracking REST queries, OpenAPI, drift test
- Fri 4: SSE endpoint plus heartbeat
- Sat 5: Angular scaffold (Tailwind, typed client), list page, SSE service
**W4 (7-12 Dec, 7 h) + Mon 14 buffer**
- Mon 7: live updates into the list, reconnect logic
- Wed 9: simulator profile
- Fri 11: full-stack compose, smoke run
- Sat 12: MVP freeze, cross-service end-to-end test, ADRs, tag `v0.1-mvp`
- Mon 14: buffer
**W5 (16-19 Dec, 5.5 h)**
- Wed 16: Dockerfiles, GitHub Actions CI
- Fri 18: AWS budget alert, IAM/OIDC, ECR push workflow
- Sat 19: Terraform for network, RDS, SSM, Kafka EC2
**W6 (21-26 Dec, 7 h)**
- Mon 21: ECS cluster, task definitions, services, ALB routing
- Wed 23: first end-to-end deploy and debugging
- Fri 25: CD workflow plus smoke test
- Sat 26: monitoring (actuator, CloudWatch alarms, local Grafana), detail page if time
**W7 (28 Dec-2 Jan, 7 h)**
- Mon 28: README, architecture diagram, run instructions
- Wed 30: demo video, API key, simulator limits, teardown script
- Fri 1 Jan: buffer
- Sat 2 Jan: fresh-clone test from the README, tag `v1.0`. Deadline Sun 3 Jan.
**If behind, cut in this order:** Terraform (write a manual runbook), Grafana, detail page/KPIs, CloudWatch alarms. Fallback deploy: everything on one EC2 with compose.
**Never cut:** outbox, idempotent consumer, tests, contract checks, README.
**Stretch, only if MVP is tagged by 12 Dec and AWS works by 23 Dec:** notification-service, Pact, tracing.