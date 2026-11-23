package com.parcelplus.shipment.api;

import com.parcelplus.shipment.domain.ServiceLevel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateShipmentRequest(
        @NotNull @Pattern(regexp = "HUB-\\d{3}") String originHub,
        @NotNull @Pattern(regexp = "HUB-\\d{3}") String destinationHub,
        @NotNull ServiceLevel serviceLevel) {

    // Turns "origin equals destination" into a 400 instead of an IllegalArgumentException (500) from the entity.
    @AssertTrue(message = "originHub and destinationHub must differ")
    boolean isHubsDifferent() {
        return originHub == null || !originHub.equals(destinationHub);
    }
}
