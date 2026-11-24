package com.parcelplus.shipment.application;

import com.parcelplus.shipment.domain.ServiceLevel;
import com.parcelplus.shipment.domain.Shipment;
import com.parcelplus.shipment.domain.ShipmentStatus;
import com.parcelplus.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipmentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    private final ShipmentRepository repository = mock(ShipmentRepository.class);
    private final ShipmentApplicationService service =
            new ShipmentApplicationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsShipmentWithTrackingCodeAndClockTime() {
        when(repository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment s = service.createShipment("HUB-001", "HUB-002", ServiceLevel.EXPRESS);

        assertThat(s.getTrackingCode()).matches("PP[0-9A-F]{10}");
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(s.getServiceLevel()).isEqualTo(ServiceLevel.EXPRESS);
        assertThat(s.getCreatedAt()).isEqualTo(NOW);
        assertThat(s.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void truncatesTimestampsToMicrosecondsLikeThePostgresColumn() {
        Instant withNanos = Instant.parse("2026-11-23T10:00:00.123456789Z");
        ShipmentApplicationService nanoService =
                new ShipmentApplicationService(repository, Clock.fixed(withNanos, ZoneOffset.UTC));
        when(repository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment s = nanoService.createShipment("HUB-001", "HUB-002", ServiceLevel.STANDARD);

        assertThat(s.getCreatedAt()).isEqualTo(Instant.parse("2026-11-23T10:00:00.123456Z"));
    }

    @Test
    void returnsExistingShipment() {
        Shipment existing = Shipment.create("PP0123456789", "HUB-001", "HUB-002", ServiceLevel.STANDARD, NOW);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThat(service.getShipment(existing.getId())).isSameAs(existing);
    }

    @Test
    void throwsWhenShipmentIsMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShipment(id))
                .isInstanceOf(ShipmentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
