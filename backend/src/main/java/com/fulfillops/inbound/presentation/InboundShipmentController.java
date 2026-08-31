package com.fulfillops.inbound.presentation;

import com.fulfillops.inbound.application.InboundShipmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments")
public class InboundShipmentController {
    private final InboundShipmentService shipmentService;
    public InboundShipmentController(InboundShipmentService shipmentService) { this.shipmentService = shipmentService; }
    @PostMapping
    public ResponseEntity<InboundShipmentResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @Valid @RequestBody CreateInboundShipmentRequest request) {
        InboundShipmentResponse response = shipmentService.create(tenantId, warehouseId, request);
        URI location = URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/inbound-shipments/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
    @GetMapping("/{shipmentId}")
    public InboundShipmentResponse getById(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID shipmentId) {
        return shipmentService.getById(tenantId, warehouseId, shipmentId);
    }
}
