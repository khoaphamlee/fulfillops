package com.fulfillops.inbound.receiving.infrastructure;

import com.fulfillops.inbound.receiving.domain.ReceivingReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivingReceiptRepository extends JpaRepository<ReceivingReceipt, UUID> {
    Optional<ReceivingReceipt> findByIdAndTenantIdAndInboundShipmentId(UUID id, UUID tenantId, UUID inboundShipmentId);
}
