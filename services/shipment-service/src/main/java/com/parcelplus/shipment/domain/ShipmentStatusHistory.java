package com.parcelplus.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of a shipment's status history. Append-only: created once, never updated.
 * Implements Persistable so Spring Data calls persist() (INSERT only) instead of merge() (SELECT, then INSERT).
 */
@Entity
@Table(name = "shipment_status_history")
public class ShipmentStatusHistory implements Persistable<UUID> {

    @Id private UUID id;

    @Column(name = "shipment_id", nullable = false, updatable = false) private UUID shipmentId;
    @Column(name = "sequence_no", nullable = false, updatable = false) private int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false) private ShipmentStatus status;

    @Column(name = "hub_code", nullable = false, updatable = false) private String hubCode;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;

    // Not a column. True until the entity has been inserted or loaded.
    @Transient private boolean newEntity = true;

    protected ShipmentStatusHistory() { } // required by JPA

    public static ShipmentStatusHistory record(UUID shipmentId, int sequenceNo, ShipmentStatus status,
                                               String hubCode, Instant occurredAt) {
        ShipmentStatusHistory h = new ShipmentStatusHistory();
        h.id = UUID.randomUUID();
        h.shipmentId = shipmentId;
        h.sequenceNo = sequenceNo;
        h.status = status;
        h.hubCode = hubCode;
        h.occurredAt = occurredAt;
        return h;
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
    public UUID getShipmentId() { return shipmentId; }
    public int getSequenceNo() { return sequenceNo; }
    public ShipmentStatus getStatus() { return status; }
    public String getHubCode() { return hubCode; }
    public Instant getOccurredAt() { return occurredAt; }
}
