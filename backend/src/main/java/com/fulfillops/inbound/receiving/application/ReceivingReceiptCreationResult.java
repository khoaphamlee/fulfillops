package com.fulfillops.inbound.receiving.application;

import com.fulfillops.inbound.receiving.presentation.ReceivingReceiptResponse;

public record ReceivingReceiptCreationResult(ReceivingReceiptResponse response, boolean replayed) {
}
