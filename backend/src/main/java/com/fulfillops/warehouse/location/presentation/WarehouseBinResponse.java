package com.fulfillops.warehouse.location.presentation;

import java.time.Instant;
import java.util.UUID;

public record WarehouseBinResponse(UUID id, UUID rackId, String code, Instant createdAt) {
}
