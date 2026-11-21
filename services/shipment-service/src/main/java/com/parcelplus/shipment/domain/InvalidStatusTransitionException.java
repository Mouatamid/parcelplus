package com.parcelplus.shipment.domain;

import java.util.UUID;

public class InvalidStatusTransitionException extends RuntimeException {

    private final UUID shipmentId;
    private final ShipmentStatus current;
    private final ShipmentStatus requested;

    public InvalidStatusTransitionException(UUID shipmentId, ShipmentStatus current, ShipmentStatus requested) {
        super("Shipment %s cannot move from %s to %s".formatted(shipmentId, current, requested));
        this.shipmentId = shipmentId;
        this.current = current;
        this.requested = requested;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public ShipmentStatus getCurrent() {
        return current;
    }

    public ShipmentStatus getRequested() {
        return requested;
    }
}
