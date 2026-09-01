package com.fulfillops.inventory.application;

import java.util.UUID;

public record ReceivedSkuIncrement(UUID skuId, long receivedQuantity) {
    public ReceivedSkuIncrement {
        if (skuId == null || receivedQuantity <= 0) throw new IllegalArgumentException("Receiving inventory increments require a SKU and positive quantity.");
    }
}
