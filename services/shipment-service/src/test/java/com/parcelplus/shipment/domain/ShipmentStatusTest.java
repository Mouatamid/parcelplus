package com.parcelplus.shipment.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.parcelplus.shipment.domain.ShipmentStatus.CREATED;
import static com.parcelplus.shipment.domain.ShipmentStatus.DELIVERED;
import static com.parcelplus.shipment.domain.ShipmentStatus.EXCEPTION;
import static com.parcelplus.shipment.domain.ShipmentStatus.IN_TRANSIT;
import static com.parcelplus.shipment.domain.ShipmentStatus.OUT_FOR_DELIVERY;
import static com.parcelplus.shipment.domain.ShipmentStatus.PICKED_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ShipmentStatusTest {

    static Stream<Arguments> validTransitions() {
        return Stream.of(
            arguments(CREATED, PICKED_UP), arguments(CREATED, EXCEPTION),
            arguments(PICKED_UP, IN_TRANSIT), arguments(PICKED_UP, EXCEPTION),
            arguments(IN_TRANSIT, IN_TRANSIT), arguments(IN_TRANSIT, OUT_FOR_DELIVERY),
            arguments(IN_TRANSIT, EXCEPTION),
            arguments(OUT_FOR_DELIVERY, DELIVERED), arguments(OUT_FOR_DELIVERY, EXCEPTION),
            arguments(EXCEPTION, IN_TRANSIT), arguments(EXCEPTION, OUT_FOR_DELIVERY));
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void allowsDeclaredTransitions(ShipmentStatus from, ShipmentStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @Test
    void rejectsEveryOtherTransition() {
        Set<List<Object>> valid = validTransitions()
            .map(a -> List.of(a.get()[0], a.get()[1])).collect(Collectors.toSet());
        for (ShipmentStatus from : ShipmentStatus.values()) {
            for (ShipmentStatus to : ShipmentStatus.values()) {
                if (!valid.contains(List.of(from, to))) {
                    assertThat(from.canTransitionTo(to)).as("%s -> %s", from, to).isFalse();
                }
            }
        }
    }
}
