package com.parcelplus.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id private UUID id;

    @Column(name = "tracking_code", nullable = false, updatable = false) private String trackingCode;
    @Column(name = "origin_hub", nullable = false, updatable = false) private String originHub;
    @Column(name = "destination_hub", nullable = false, updatable = false) private String destinationHub;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_level", nullable = false, updatable = false) private ServiceLevel serviceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ShipmentStatus status;

    @Column(name = "last_sequence", nullable = false) private int lastSequence;

    @Version private Long version;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Shipment() { } // required by JPA

    public static Shipment create(String trackingCode, String originHub, String destinationHub,
                                  ServiceLevel serviceLevel, Instant now) {
        if (originHub.equals(destinationHub)) {
            throw new IllegalArgumentException("origin and destination hubs must differ");
        }
        Shipment s = new Shipment();
        s.id = UUID.randomUUID();
        s.trackingCode = trackingCode;
        s.originHub = originHub;
        s.destinationHub = destinationHub;
        s.serviceLevel = serviceLevel;
        s.status = ShipmentStatus.CREATED;
        s.lastSequence = 0;
        s.createdAt = now;
        s.updatedAt = now;
        return s;
    }

    public void changeStatus(ShipmentStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(id, status, next);
        }
        this.status = next;
        this.lastSequence++;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getTrackingCode() { return trackingCode; }
    public String getOriginHub() { return originHub; }
    public String getDestinationHub() { return destinationHub; }
    public ServiceLevel getServiceLevel() { return serviceLevel; }
    public ShipmentStatus getStatus() { return status; }
    public int getLastSequence() { return lastSequence; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}