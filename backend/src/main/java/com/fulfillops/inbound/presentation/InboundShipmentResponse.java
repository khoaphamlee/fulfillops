package com.fulfillops.inbound.presentation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InboundShipmentResponse(UUID id, UUID tenantId, UUID warehouseId, Instant createdAt, List<InboundShipmentLineResponse> lines) {
}
