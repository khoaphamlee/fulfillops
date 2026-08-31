package com.fulfillops.membership.presentation;

import java.time.Instant;
import java.util.UUID;

public record TenantMembershipResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        Instant createdAt) {
}
