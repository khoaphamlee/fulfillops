package com.fulfillops.inbound.receiving.application;

import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReceivingCommandCoordinator {
    private final ReceivingService receivingService;
    private final ReceivingReplayLookupService replayLookupService;

    public ReceivingCommandCoordinator(ReceivingService receivingService, ReceivingReplayLookupService replayLookupService) {
        this.receivingService = receivingService;
        this.replayLookupService = replayLookupService;
    }

    public ReceivingReceiptCreationResult create(UUID tenantId, UUID warehouseId, UUID shipmentId, String idempotencyKey, CreateReceivingReceiptRequest request) {
        try {
            return receivingService.create(tenantId, warehouseId, shipmentId, idempotencyKey, request);
        } catch (ReceivingIdempotencyUniqueRaceException exception) {
            return replayLookupService.find(tenantId, shipmentId, idempotencyKey, exception.getRequestFingerprint())
                    .orElseThrow(exception::getPersistenceException);
        }
    }
}
