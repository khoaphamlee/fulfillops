package com.fulfillops.tenant.presentation;

import com.fulfillops.tenant.domain.TenantStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String code,
        String name,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
