package com.parcelplus.shipment.repository;

import com.parcelplus.shipment.domain.Shipment;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Extends the bare Repository marker, not JpaRepository, so only the methods
 * declared here exist. No findAll, no delete: this service never lists or removes.
 */
public interface ShipmentRepository extends Repository<Shipment, UUID> {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(UUID id);
}
