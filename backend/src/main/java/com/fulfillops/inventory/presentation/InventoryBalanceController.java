package com.fulfillops.inventory.presentation;

import com.fulfillops.inventory.application.InventoryBalanceResponse;
import com.fulfillops.inventory.application.InventoryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inventory")
public class InventoryBalanceController {
    private final InventoryService inventoryService;
    public InventoryBalanceController(InventoryService inventoryService) { this.inventoryService = inventoryService; }

    @GetMapping("/skus/{skuId}")
    public InventoryBalanceResponse getBySku(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID skuId) {
        return inventoryService.getBalance(tenantId, warehouseId, skuId);
    }
}
