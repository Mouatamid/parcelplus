package com.parcelplus.shipment.api;

import com.parcelplus.shipment.domain.ServiceLevel;
import com.parcelplus.shipment.domain.Shipment;
import com.parcelplus.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String trackingCode,
        String originHub,
        String destinationHub,
        ServiceLevel serviceLevel,
        ShipmentStatus status,
        int lastSequence,
        Instant createdAt,
        Instant updatedAt) {

    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(
                s.getId(), s.getTrackingCode(), s.getOriginHub(), s.getDestinationHub(),
                s.getServiceLevel(), s.getStatus(), s.getLastSequence(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
