package com.fulfillops.membership.presentation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTenantMembershipRequest(@NotNull UUID userId) {
}
