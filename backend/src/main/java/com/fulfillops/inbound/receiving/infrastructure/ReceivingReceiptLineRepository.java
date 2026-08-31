package com.fulfillops.inbound.receiving.infrastructure;

import com.fulfillops.inbound.receiving.domain.ReceivingReceiptLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivingReceiptLineRepository extends JpaRepository<ReceivingReceiptLine, UUID> {
    List<ReceivingReceiptLine> findByReceivingReceiptId(UUID receivingReceiptId);
    List<ReceivingReceiptLine> findByTenantIdAndInboundShipmentId(UUID tenantId, UUID inboundShipmentId);
}
