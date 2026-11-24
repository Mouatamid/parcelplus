package com.parcelplus.shipment.api;

import com.parcelplus.shipment.application.ShipmentApplicationService;
import com.parcelplus.shipment.domain.Shipment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

// The annotations below only describe the API for the generated OpenAPI file. They do not change behavior.
@RestController
@RequestMapping(path = "/api/shipments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "shipments")
public class ShipmentController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ShipmentApplicationService service;

    public ShipmentController(ShipmentApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(operationId = "createShipment", summary = "Create a shipment in status CREATED")
    @ApiResponse(responseCode = "201", description = "Shipment created",
            headers = @Header(name = "Location", description = "URL of the new shipment",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        Shipment shipment = service.createShipment(
                request.originHub(), request.destinationHub(), request.serviceLevel());
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(shipment.getId()).toUri();
        return ResponseEntity.created(location).body(ShipmentResponse.from(shipment));
    }

    @PostMapping("/{id}/status-changes")
    @Operation(operationId = "changeShipmentStatus", summary = "Move a shipment to the next status")
    @ApiResponse(responseCode = "200", description = "Status changed; lastSequence is incremented")
    @ApiResponse(responseCode = "400", description = "Validation failed, or the id is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No shipment with this id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Transition not allowed (adds shipmentId, currentStatus, requestedStatus), "
                    + "or the shipment was changed concurrently",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ShipmentResponse changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeStatusRequest request) {
        return ShipmentResponse.from(service.changeStatus(id, request.status()));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getShipment", summary = "Get one shipment by id")
    @ApiResponse(responseCode = "200", description = "The shipment")
    @ApiResponse(responseCode = "400", description = "The id is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No shipment with this id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ShipmentResponse get(@PathVariable UUID id) {
        return ShipmentResponse.from(service.getShipment(id));
    }
}
