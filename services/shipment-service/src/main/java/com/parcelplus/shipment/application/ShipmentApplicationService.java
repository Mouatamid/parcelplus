package com.parcelplus.shipment.application;

import com.parcelplus.shipment.domain.ServiceLevel;
import com.parcelplus.shipment.domain.Shipment;
import com.parcelplus.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ShipmentApplicationService {

    private final ShipmentRepository repository;
    private final Clock clock;

    public ShipmentApplicationService(ShipmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Shipment createShipment(String originHub, String destinationHub, ServiceLevel serviceLevel) {
        Shipment shipment = Shipment.create(
                newTrackingCode(), originHub, destinationHub, serviceLevel, Instant.now(clock));
        return repository.save(shipment);
    }

    @Transactional(readOnly = true)
    public Shipment getShipment(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    // "PP" + first 10 hex chars of a random UUID, e.g. PP3F9A01C7BE
    private static String newTrackingCode() {
        String hex = UUID.randomUUID().toString().replace("-", "");
        return "PP" + hex.substring(0, 10).toUpperCase();
    }
}
