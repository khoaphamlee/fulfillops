package com.fulfillops.inventory.application;

import java.util.UUID;

public record ReceivingInventoryMovement(UUID receivingReceiptLineId, UUID skuId, long receivedQuantity) {
    public ReceivingInventoryMovement {
        if (receivingReceiptLineId == null || skuId == null || receivedQuantity <= 0) {
            throw new IllegalArgumentException("Receiving inventory movements require ReceiptLine provenance, a SKU, and positive quantity.");
        }
    }
}
