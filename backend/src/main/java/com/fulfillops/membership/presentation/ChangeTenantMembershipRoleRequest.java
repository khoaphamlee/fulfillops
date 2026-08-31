package com.fulfillops.membership.presentation;

import com.fulfillops.membership.domain.TenantRole;
import jakarta.validation.constraints.NotNull;

public record ChangeTenantMembershipRoleRequest(@NotNull TenantRole role) {
}
