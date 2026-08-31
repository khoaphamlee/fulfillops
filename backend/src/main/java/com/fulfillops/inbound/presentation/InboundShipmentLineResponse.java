package com.fulfillops.inbound.presentation;

import java.time.Instant;
import java.util.UUID;

public record InboundShipmentLineResponse(UUID id, UUID skuId, long expectedQuantity, Instant createdAt) {
}
