package com.parcelplus.shipment.application;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {

    private final UUID shipmentId;

    public ShipmentNotFoundException(UUID shipmentId) {
        super("Shipment %s not found".formatted(shipmentId));
        this.shipmentId = shipmentId;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }
}
