package com.fulfillops.inbound.infrastructure;

import com.fulfillops.inbound.domain.InboundShipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundShipmentRepository extends JpaRepository<InboundShipment, UUID> {
    Optional<InboundShipment> findByIdAndTenantIdAndWarehouseId(UUID id, UUID tenantId, UUID warehouseId);
}
