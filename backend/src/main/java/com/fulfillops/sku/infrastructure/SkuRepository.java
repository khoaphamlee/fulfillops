package com.fulfillops.sku.infrastructure;

import com.fulfillops.sku.domain.Sku;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, UUID> {

    Optional<Sku> findByIdAndTenantId(UUID id, UUID tenantId);
}
