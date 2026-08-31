package com.fulfillops.inbound.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateInboundShipmentLineRequest(@NotNull UUID skuId, @NotNull @Positive Long expectedQuantity) {
}
