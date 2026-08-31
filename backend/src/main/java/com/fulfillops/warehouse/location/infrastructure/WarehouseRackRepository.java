package com.fulfillops.warehouse.location.infrastructure;

import com.fulfillops.warehouse.location.domain.WarehouseRack;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRackRepository extends JpaRepository<WarehouseRack, UUID> {
    Optional<WarehouseRack> findByIdAndAisleId(UUID id, UUID aisleId);
}
