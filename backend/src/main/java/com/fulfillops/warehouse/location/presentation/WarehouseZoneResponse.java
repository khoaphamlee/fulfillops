package com.fulfillops.warehouse.location.presentation;

import java.time.Instant;
import java.util.UUID;

public record WarehouseZoneResponse(UUID id, UUID warehouseId, String code, Instant createdAt) {
}
