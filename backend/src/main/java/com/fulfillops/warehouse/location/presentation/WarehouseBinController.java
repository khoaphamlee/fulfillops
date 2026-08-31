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
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/racks/{rackId}/bins")
public class WarehouseBinController {
    private final WarehouseLocationService locationService;
    public WarehouseBinController(WarehouseLocationService locationService) { this.locationService = locationService; }
    @PostMapping
    public ResponseEntity<WarehouseBinResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID rackId, @Valid @RequestBody CreateWarehouseBinRequest request) {
        WarehouseBinResponse response = locationService.createBin(tenantId, warehouseId, rackId, request);
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/racks/" + rackId + "/bins/" + response.id())).body(response);
    }
    @GetMapping("/{binId}")
    public WarehouseBinResponse get(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID rackId, @PathVariable UUID binId) {
        return locationService.getBin(tenantId, warehouseId, rackId, binId);
    }
}
