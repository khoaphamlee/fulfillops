package com.fulfillops.inventory.ledger.infrastructure;

import com.fulfillops.inventory.ledger.domain.InventoryLedgerEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLedgerEntryRepository extends JpaRepository<InventoryLedgerEntry, UUID> {
    long countByTenantIdAndWarehouseIdAndSkuId(UUID tenantId, UUID warehouseId, UUID skuId);
}
