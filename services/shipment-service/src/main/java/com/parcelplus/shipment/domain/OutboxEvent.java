package com.parcelplus.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A row of the transactional outbox. The id is the event's eventId, assigned by the caller.
 * Implements Persistable so Spring Data calls persist() (INSERT only) instead of merge() (SELECT, then INSERT).
 */
@Entity
@Table(name = "outbox")
public class OutboxEvent implements Persistable<UUID> {

    @Id private UUID id;

    @Column(name = "aggregate_id", nullable = false, updatable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, updatable = false) private String eventType;

    // The already-serialized event as JSON text. SqlTypes.JSON makes Hibernate bind it as jsonb;
    // without it Postgres rejects a varchar parameter for a jsonb column.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false) private String payload;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    // Null until the poller has published the row to Kafka.
    @Column(name = "published_at") private Instant publishedAt;

    // Not a column. True until the entity has been inserted or loaded.
    @Transient private boolean newEntity = true;

    protected OutboxEvent() { } // required by JPA

    public static OutboxEvent create(UUID eventId, UUID aggregateId, String eventType,
                                     String payloadJson, Instant now) {
        OutboxEvent e = new OutboxEvent();
        e.id = eventId;
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payload = payloadJson;
        e.createdAt = now;
        return e;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    @Override public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
