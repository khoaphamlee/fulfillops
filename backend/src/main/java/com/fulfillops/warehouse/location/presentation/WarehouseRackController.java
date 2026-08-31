package com.fulfillops.warehouse.location.presentation;

import com.fulfillops.warehouse.location.application.WarehouseLocationService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/aisles/{aisleId}/racks")
public class WarehouseRackController {
    private final WarehouseLocationService locationService;
    public WarehouseRackController(WarehouseLocationService locationService) { this.locationService = locationService; }
    @PostMapping
    public ResponseEntity<WarehouseRackResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID aisleId, @Valid @RequestBody CreateWarehouseRackRequest request) {
        WarehouseRackResponse response = locationService.createRack(tenantId, warehouseId, aisleId, request);
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/aisles/" + aisleId + "/racks/" + response.id())).body(response);
    }
    @GetMapping("/{rackId}")
    public WarehouseRackResponse get(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID aisleId, @PathVariable UUID rackId) {
        return locationService.getRack(tenantId, warehouseId, aisleId, rackId);
    }
}
