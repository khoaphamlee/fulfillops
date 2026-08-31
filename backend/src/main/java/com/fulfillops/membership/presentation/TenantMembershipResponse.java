package com.fulfillops.membership.presentation;

import com.fulfillops.membership.domain.TenantRole;
import java.time.Instant;
import java.util.UUID;

public record TenantMembershipResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        TenantRole role,
        Instant createdAt,
        Instant updatedAt) {
}
