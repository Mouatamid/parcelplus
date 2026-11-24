package com.parcelplus.shipment.repository;

import com.parcelplus.shipment.TestcontainersConfiguration;
import com.parcelplus.shipment.domain.ServiceLevel;
import com.parcelplus.shipment.domain.Shipment;
import com.parcelplus.shipment.domain.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShipmentOptimisticLockingIT {

    @Autowired
    ShipmentRepository repository;

    @Autowired
    TransactionTemplate tx;

    @Test
    void secondSaveOfStaleCopyFailsWithOptimisticLockingException() {
        UUID id = tx.execute(status -> repository.save(newShipment())).getId();

        // Two separate transactions, each returns its own detached copy at version 0.
        Shipment copyA = tx.execute(status -> repository.findById(id).orElseThrow());
        Shipment copyB = tx.execute(status -> repository.findById(id).orElseThrow());
        assertThat(copyA).isNotSameAs(copyB);
        assertThat(copyA.getVersion()).isEqualTo(copyB.getVersion());

        // Both transitions are legal from CREATED, so only the version can cause a failure.
        copyA.changeStatus(ShipmentStatus.PICKED_UP, Instant.parse("2026-11-24T10:00:00Z"));
        copyB.changeStatus(ShipmentStatus.EXCEPTION, Instant.parse("2026-11-24T10:00:01Z"));

        tx.executeWithoutResult(status -> repository.save(copyA));

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.save(copyB)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // The first save won; the second left no trace.
        Shipment stored = tx.execute(status -> repository.findById(id).orElseThrow());
        assertThat(stored.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(stored.getLastSequence()).isEqualTo(1);
        assertThat(stored.getVersion()).isEqualTo(copyA.getVersion() + 1);
    }

    private static Shipment newShipment() {
        String code = "PP" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return Shipment.create(code, "HUB-001", "HUB-002", ServiceLevel.STANDARD,
                Instant.parse("2026-11-24T09:00:00Z"));
    }
}
