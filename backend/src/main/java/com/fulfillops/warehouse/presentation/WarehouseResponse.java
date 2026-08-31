package com.fulfillops.warehouse.presentation;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        Instant createdAt) {
}
