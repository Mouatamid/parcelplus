package com.parcelplus.shipment.repository;

import com.parcelplus.shipment.domain.ShipmentStatusHistory;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/** Append-only: insert and read by id. No update, no delete, no listing. */
public interface ShipmentStatusHistoryRepository extends Repository<ShipmentStatusHistory, UUID> {

    ShipmentStatusHistory save(ShipmentStatusHistory entry);

    Optional<ShipmentStatusHistory> findById(UUID id);
}
