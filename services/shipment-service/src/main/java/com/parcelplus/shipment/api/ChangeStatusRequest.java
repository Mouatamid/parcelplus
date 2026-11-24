package com.parcelplus.shipment.api;

import com.parcelplus.shipment.domain.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangeStatusRequest(
        @NotNull ShipmentStatus status,
        @NotNull @Pattern(regexp = "HUB-\\d{3}") String hubCode) {
}
