package com.fulfillops.inbound.receiving.presentation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ReceivingReceiptResponse(UUID id, UUID tenantId, UUID inboundShipmentId, Instant createdAt, List<ReceivingReceiptLineResponse> lines) {}
