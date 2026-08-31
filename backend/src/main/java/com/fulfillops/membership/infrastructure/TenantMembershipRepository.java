package com.fulfillops.membership.infrastructure;

import com.fulfillops.membership.domain.TenantMembership;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    Optional<TenantMembership> findByIdAndTenantId(UUID id, UUID tenantId);
}
