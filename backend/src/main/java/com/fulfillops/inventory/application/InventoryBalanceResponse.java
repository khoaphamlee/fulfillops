package com.fulfillops.inventory.application;

import java.time.Instant;
import java.util.UUID;

public record InventoryBalanceResponse(UUID tenantId, UUID warehouseId, UUID skuId, long onHandQuantity, Instant createdAt, Instant updatedAt) {
}
