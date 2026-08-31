package com.fulfillops.inbound.infrastructure;

import com.fulfillops.inbound.domain.InboundShipmentLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundShipmentLineRepository extends JpaRepository<InboundShipmentLine, UUID> {
    List<InboundShipmentLine> findByInboundShipmentId(UUID inboundShipmentId);
}
