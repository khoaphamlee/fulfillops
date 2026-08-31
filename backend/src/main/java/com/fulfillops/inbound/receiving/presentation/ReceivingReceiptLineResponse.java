package com.fulfillops.inbound.receiving.presentation;
import java.time.Instant;
import java.util.UUID;
public record ReceivingReceiptLineResponse(UUID id, UUID inboundShipmentLineId, long receivedQuantity, long cumulativeReceivedQuantity, Instant createdAt) {}
