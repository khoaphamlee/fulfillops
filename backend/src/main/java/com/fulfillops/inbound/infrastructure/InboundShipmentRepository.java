package com.fulfillops.inbound.infrastructure;

import com.fulfillops.inbound.domain.InboundShipment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundShipmentRepository extends JpaRepository<InboundShipment, UUID> {
    Optional<InboundShipment> findByIdAndTenantIdAndWarehouseId(UUID id, UUID tenantId, UUID warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from InboundShipment shipment where shipment.id = ?1 and shipment.tenantId = ?2 and shipment.warehouseId = ?3")
    Optional<InboundShipment> findScopedForUpdate(UUID id, UUID tenantId, UUID warehouseId);
}
