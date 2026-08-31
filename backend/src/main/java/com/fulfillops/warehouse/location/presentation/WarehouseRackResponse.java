package com.fulfillops.warehouse.location.presentation;

import java.time.Instant;
import java.util.UUID;

public record WarehouseRackResponse(UUID id, UUID aisleId, String code, Instant createdAt) {
}
