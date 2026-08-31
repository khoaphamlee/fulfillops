package com.fulfillops.warehouse.location.infrastructure;

import com.fulfillops.warehouse.location.domain.WarehouseZone;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseZoneRepository extends JpaRepository<WarehouseZone, UUID> {
    Optional<WarehouseZone> findByIdAndWarehouseId(UUID id, UUID warehouseId);
}
