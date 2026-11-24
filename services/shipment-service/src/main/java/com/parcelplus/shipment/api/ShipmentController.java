package com.parcelplus.shipment.api;

import com.parcelplus.shipment.application.ShipmentApplicationService;
import com.parcelplus.shipment.domain.Shipment;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentApplicationService service;

    public ShipmentController(ShipmentApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        Shipment shipment = service.createShipment(
                request.originHub(), request.destinationHub(), request.serviceLevel());
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(shipment.getId()).toUri();
        return ResponseEntity.created(location).body(ShipmentResponse.from(shipment));
    }

    @PostMapping("/{id}/status-changes")
    public ShipmentResponse changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeStatusRequest request) {
        return ShipmentResponse.from(service.changeStatus(id, request.status()));
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable UUID id) {
        return ShipmentResponse.from(service.getShipment(id));
    }
}
