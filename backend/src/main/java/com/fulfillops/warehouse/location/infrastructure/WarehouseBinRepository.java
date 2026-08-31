package com.fulfillops.warehouse.location.infrastructure;

import com.fulfillops.warehouse.location.domain.WarehouseBin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseBinRepository extends JpaRepository<WarehouseBin, UUID> {
    Optional<WarehouseBin> findByIdAndRackId(UUID id, UUID rackId);
}
