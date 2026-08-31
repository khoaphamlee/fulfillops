package com.fulfillops.warehouse.location.presentation;

import java.time.Instant;
import java.util.UUID;

public record WarehouseAisleResponse(UUID id, UUID zoneId, String code, Instant createdAt) {
}
