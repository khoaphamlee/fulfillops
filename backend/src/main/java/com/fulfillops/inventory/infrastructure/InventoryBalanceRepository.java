package com.fulfillops.inventory.infrastructure;

import com.fulfillops.inventory.domain.InventoryBalance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {
    Optional<InventoryBalance> findByTenantIdAndWarehouseIdAndSkuId(UUID tenantId, UUID warehouseId, UUID skuId);
    long countByTenantIdAndWarehouseIdAndSkuId(UUID tenantId, UUID warehouseId, UUID skuId);
}
