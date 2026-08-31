package com.fulfillops.warehouse.location.infrastructure;

import com.fulfillops.warehouse.location.domain.WarehouseAisle;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseAisleRepository extends JpaRepository<WarehouseAisle, UUID> {
    Optional<WarehouseAisle> findByIdAndZoneId(UUID id, UUID zoneId);
}
