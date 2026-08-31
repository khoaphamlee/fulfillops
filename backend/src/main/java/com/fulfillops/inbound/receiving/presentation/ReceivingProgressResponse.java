package com.fulfillops.inbound.receiving.presentation;
import java.util.List;
import java.util.UUID;
public record ReceivingProgressResponse(UUID inboundShipmentId, List<ReceivingProgressLineResponse> lines) {}
