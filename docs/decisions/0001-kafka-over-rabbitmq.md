# 0001. Kafka over RabbitMQ
Date: 2026-11-19. Status: accepted

## Context
shipment-service publishes scan events. tracking-service builds a read model from them.
- Events for one shipment must be applied in order: `ShipmentCreated` (sequence 0) before any `ShipmentStatusChanged`.
- Delivery is at-least-once, so the consumer is idempotent and uses `sequence` to detect duplicates and gaps.
- The read model (`tracking_db`) must be rebuildable from history, for example after a consumer bug.
- More consumers may be added later (the stretch notification-service) without changing the producer.
- One developer, local runs through Docker Compose, a fixed deadline.

## Decision
Use Apache Kafka (KRaft mode, no ZooKeeper), one broker.
One topic `shipment.events.v1`, key = `shipmentId`, 3 partitions, replication factor 1, plus a dead-letter topic `shipment.events.v1.dlt`.

## Alternatives considered
- **RabbitMQ (classic queues).** Simpler to run and a good fit for task distribution. It lost because
  a message is removed once acknowledged, so there is no replay, and ordering is only kept with a single
  consumer per queue. Rebuilding the read model and adding a second independent consumer would both need extra work.
- **RabbitMQ Streams.** Gives a log with replay, so it narrows the gap. It lost because it is a newer,
  less common part of RabbitMQ and I would be using the product against its main model.
- **AWS SQS FIFO.** Ordering per message group, managed, cheap. It lost because it has no replay,
  it does not run locally without an emulator, and it ties the design to AWS.
- **No broker (modular monolith).** The honest production answer for a system this size, see `architecture.md` section 2.
  It lost because the project exists to demonstrate event-driven design.

## Consequences
Easier:
- Per-shipment ordering: same key, same partition.
- Replay: reset the consumer offset and rebuild `tracking_db`.
- A new consumer group reads the same topic without touching the producer.

Harder, and accepted:
- More concepts to learn and defend: partitions, offsets, consumer groups, rebalancing.
- Ordering exists only inside one shipment, not across shipments.
- 3 partitions means at most 3 active consumers per group.
- Replication factor 1 on a single broker: losing the broker loses the log. Acceptable for a demo, not for production (see 0002).
- At-least-once delivery forces the idempotent consumer and the sequence rules.
