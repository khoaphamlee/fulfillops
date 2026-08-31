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
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones")
public class WarehouseZoneController {
    private final WarehouseLocationService locationService;
    public WarehouseZoneController(WarehouseLocationService locationService) { this.locationService = locationService; }
    @PostMapping
    public ResponseEntity<WarehouseZoneResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @Valid @RequestBody CreateWarehouseZoneRequest request) {
        WarehouseZoneResponse response = locationService.createZone(tenantId, warehouseId, request);
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/zones/" + response.id())).body(response);
    }
    @GetMapping("/{zoneId}")
    public WarehouseZoneResponse get(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID zoneId) {
        return locationService.getZone(tenantId, warehouseId, zoneId);
    }
}
