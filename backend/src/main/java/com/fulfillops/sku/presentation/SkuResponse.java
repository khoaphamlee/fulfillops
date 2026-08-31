package com.fulfillops.sku.presentation;

import java.time.Instant;
import java.util.UUID;

public record SkuResponse(UUID id, UUID tenantId, String code, String name, Instant createdAt) {
}
