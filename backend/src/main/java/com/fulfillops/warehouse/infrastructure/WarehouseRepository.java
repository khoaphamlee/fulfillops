package com.fulfillops.warehouse.infrastructure;

import com.fulfillops.warehouse.domain.Warehouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    Optional<Warehouse> findByIdAndTenantId(UUID id, UUID tenantId);
}
