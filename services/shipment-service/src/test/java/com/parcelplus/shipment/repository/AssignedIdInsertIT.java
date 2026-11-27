package com.parcelplus.shipment.repository;

import com.parcelplus.shipment.TestcontainersConfiguration;
import com.parcelplus.shipment.domain.OutboxEvent;
import com.parcelplus.shipment.domain.ServiceLevel;
import com.parcelplus.shipment.domain.Shipment;
import com.parcelplus.shipment.domain.ShipmentStatus;
import com.parcelplus.shipment.domain.ShipmentStatusHistory;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** History and outbox rows: INSERT-only saves, jsonb payload, constraints. Real Postgres. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AssignedIdInsertIT {

    private static final Instant NOW = Instant.parse("2026-11-27T09:00:00Z");

    @Autowired ShipmentRepository shipments;
    @Autowired ShipmentStatusHistoryRepository history;
    @Autowired OutboxRepository outbox;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void enableStatistics() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    private UUID newShipmentId() {
        String code = "PP" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return tx.execute(s -> shipments.save(
                Shipment.create(code, "HUB-001", "HUB-002", ServiceLevel.STANDARD, NOW))).getId();
    }

    @Test
    void outboxInsertIsASingleStatementWithoutSelect() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create(eventId, UUID.randomUUID(), "ShipmentStatusChanged",
                "{\"status\":\"PICKED_UP\",\"hubCode\":\"HUB-001\"}", NOW);

        stats.clear();
        tx.executeWithoutResult(s -> outbox.save(event));

        // merge() would be 2 statements: SELECT by id, then INSERT.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(outbox.findById(eventId)).get()
                .satisfies(e -> assertThat(e.getPublishedAt()).isNull());
    }

    @Test
    void payloadIsStoredAsAJsonbObjectNotAJsonString() {
        UUID eventId = UUID.randomUUID();
        tx.executeWithoutResult(s -> outbox.save(OutboxEvent.create(eventId, UUID.randomUUID(),
                "ShipmentStatusChanged", "{\"status\":\"PICKED_UP\",\"hubCode\":\"HUB-001\"}", NOW)));

        String type = jdbc.queryForObject("SELECT jsonb_typeof(payload) FROM outbox WHERE id = ?",
                String.class, eventId);
        String status = jdbc.queryForObject("SELECT payload ->> 'status' FROM outbox WHERE id = ?",
                String.class, eventId);

        assertThat(type).isEqualTo("object");
        assertThat(status).isEqualTo("PICKED_UP");
    }

    @Test
    void historyInsertIsASingleStatementWithoutSelect() {
        UUID shipmentId = newShipmentId();
        ShipmentStatusHistory entry =
                ShipmentStatusHistory.record(shipmentId, 1, ShipmentStatus.PICKED_UP, "HUB-001", NOW);

        stats.clear();
        tx.executeWithoutResult(s -> history.save(entry));

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        ShipmentStatusHistory stored = history.findById(entry.getId()).orElseThrow();
        assertThat(stored.getShipmentId()).isEqualTo(shipmentId);
        assertThat(stored.getSequenceNo()).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(stored.getHubCode()).isEqualTo("HUB-001");
        assertThat(stored.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void sameSequenceNumberTwiceForOneShipmentIsRejected() {
        UUID shipmentId = newShipmentId();
        tx.executeWithoutResult(s -> history.save(
                ShipmentStatusHistory.record(shipmentId, 1, ShipmentStatus.PICKED_UP, "HUB-001", NOW)));

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> history.save(
                ShipmentStatusHistory.record(shipmentId, 1, ShipmentStatus.EXCEPTION, "HUB-002", NOW))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void historyForUnknownShipmentIsRejected() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> history.save(
                ShipmentStatusHistory.record(UUID.randomUUID(), 0, ShipmentStatus.CREATED, "HUB-001", NOW))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
