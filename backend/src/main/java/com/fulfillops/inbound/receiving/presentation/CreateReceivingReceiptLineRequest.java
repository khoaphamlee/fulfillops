package com.fulfillops.inbound.receiving.presentation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
public record CreateReceivingReceiptLineRequest(@NotNull UUID inboundShipmentLineId, @NotNull @Positive Long receivedQuantity) {}
