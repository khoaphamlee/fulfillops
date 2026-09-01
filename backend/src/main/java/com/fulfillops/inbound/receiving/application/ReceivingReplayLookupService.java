package com.fulfillops.inbound.receiving.application;

import com.fulfillops.inbound.receiving.domain.ReceivingReceipt;
import com.fulfillops.inbound.receiving.domain.ReceivingReceiptLine;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptLineRepository;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptRepository;
import com.fulfillops.inbound.receiving.presentation.ReceivingReceiptLineResponse;
import com.fulfillops.inbound.receiving.presentation.ReceivingReceiptResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivingReplayLookupService {
    private final ReceivingReceiptRepository receiptRepository;
    private final ReceivingReceiptLineRepository receiptLineRepository;

    public ReceivingReplayLookupService(ReceivingReceiptRepository receiptRepository, ReceivingReceiptLineRepository receiptLineRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptLineRepository = receiptLineRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ReceivingReceiptCreationResult> find(UUID tenantId, UUID shipmentId, String idempotencyKey, String fingerprint) {
        return receiptRepository.findByTenantIdAndInboundShipmentIdAndIdempotencyKey(tenantId, shipmentId, idempotencyKey)
                .map(receipt -> {
                    if (!fingerprint.equals(receipt.getRequestFingerprint())) throw new ReceivingIdempotencyKeyReusedWithDifferentRequestException();
                    return new ReceivingReceiptCreationResult(toReceiptResponse(receipt), true);
                });
    }

    private ReceivingReceiptResponse toReceiptResponse(ReceivingReceipt receipt) {
        List<ReceivingReceiptLine> lines = receiptLineRepository.findByReceivingReceiptId(receipt.getId());
        Map<UUID, Long> cumulative = cumulativeQuantities(receipt.getTenantId(), receipt.getInboundShipmentId());
        List<ReceivingReceiptLineResponse> responses = lines.stream().map(line -> new ReceivingReceiptLineResponse(line.getId(), line.getInboundShipmentLineId(), line.getReceivedQuantity(), cumulative.getOrDefault(line.getInboundShipmentLineId(), 0L), line.getCreatedAt())).toList();
        return new ReceivingReceiptResponse(receipt.getId(), receipt.getTenantId(), receipt.getInboundShipmentId(), receipt.getCreatedAt(), responses);
    }

    private Map<UUID, Long> cumulativeQuantities(UUID tenantId, UUID shipmentId) {
        Map<UUID, Long> quantities = new HashMap<>();
        for (ReceivingReceiptLine line : receiptLineRepository.findByTenantIdAndInboundShipmentId(tenantId, shipmentId)) {
            quantities.merge(line.getInboundShipmentLineId(), line.getReceivedQuantity(), Long::sum);
        }
        return quantities;
    }
}
