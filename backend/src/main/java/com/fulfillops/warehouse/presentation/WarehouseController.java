package com.fulfillops.warehouse.presentation;

import com.fulfillops.warehouse.application.WarehouseService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.create(tenantId, request);
        URI location = URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{warehouseId}")
    public WarehouseResponse getById(
            @PathVariable UUID tenantId,
            @PathVariable UUID warehouseId) {
        return warehouseService.getById(tenantId, warehouseId);
    }
}
