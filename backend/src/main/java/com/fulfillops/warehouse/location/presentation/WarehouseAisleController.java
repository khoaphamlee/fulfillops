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
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones/{zoneId}/aisles")
public class WarehouseAisleController {
    private final WarehouseLocationService locationService;
    public WarehouseAisleController(WarehouseLocationService locationService) { this.locationService = locationService; }
    @PostMapping
    public ResponseEntity<WarehouseAisleResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID zoneId, @Valid @RequestBody CreateWarehouseAisleRequest request) {
        WarehouseAisleResponse response = locationService.createAisle(tenantId, warehouseId, zoneId, request);
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/zones/" + zoneId + "/aisles/" + response.id())).body(response);
    }
    @GetMapping("/{aisleId}")
    public WarehouseAisleResponse get(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID zoneId, @PathVariable UUID aisleId) {
        return locationService.getAisle(tenantId, warehouseId, zoneId, aisleId);
    }
}
