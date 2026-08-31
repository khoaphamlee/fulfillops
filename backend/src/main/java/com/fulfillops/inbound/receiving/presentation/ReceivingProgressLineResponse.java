package com.fulfillops.inbound.receiving.presentation;
import java.util.UUID;
public record ReceivingProgressLineResponse(UUID inboundShipmentLineId, UUID skuId, long expectedQuantity, long receivedQuantity, long remainingQuantity) {}
