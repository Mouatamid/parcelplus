package com.parcelplus.shipment.repository;

import com.parcelplus.shipment.domain.OutboxEvent;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/** The poller's query for unpublished rows is added with the poller, not before. */
public interface OutboxRepository extends Repository<OutboxEvent, UUID> {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);
}
