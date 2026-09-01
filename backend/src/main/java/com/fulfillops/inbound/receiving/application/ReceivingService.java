package com.fulfillops.inbound.receiving.application;

import com.fulfillops.inbound.application.InboundShipmentNotFoundException;
import com.fulfillops.inbound.domain.InboundShipment;
import com.fulfillops.inbound.domain.InboundShipmentLine;
import com.fulfillops.inbound.infrastructure.InboundShipmentLineRepository;
import com.fulfillops.inbound.infrastructure.InboundShipmentRepository;
import com.fulfillops.inbound.receiving.domain.ReceivingReceipt;
import com.fulfillops.inbound.receiving.domain.ReceivingReceiptLine;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptLineRepository;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptRepository;
import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptLineRequest;
import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptRequest;
import com.fulfillops.inbound.receiving.presentation.ReceivingProgressLineResponse;
import com.fulfillops.inbound.receiving.presentation.ReceivingProgressResponse;
import com.fulfillops.inbound.receiving.presentation.ReceivingReceiptLineResponse;
import com.fulfillops.inbound.receiving.presentation.ReceivingReceiptResponse;
import com.fulfillops.inventory.application.InventoryService;
import com.fulfillops.inventory.application.ReceivingInventoryMovement;
import com.fulfillops.warehouse.application.WarehouseService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivingService {
    private static final String DUPLICATE_LINE_CONSTRAINT = "uk_receiving_receipt_lines_receipt_planned_line";
    private static final String IDEMPOTENCY_CONSTRAINT = "uk_receiving_receipts_tenant_shipment_idempotency_key";
    private final InboundShipmentRepository shipmentRepository;
    private final InboundShipmentLineRepository plannedLineRepository;
    private final ReceivingReceiptRepository receiptRepository;
    private final ReceivingReceiptLineRepository receiptLineRepository;
    private final WarehouseService warehouseService;
    private final ReceivingRequestFingerprint requestFingerprint;
    private final InventoryService inventoryService;

    public ReceivingService(InboundShipmentRepository shipmentRepository, InboundShipmentLineRepository plannedLineRepository, ReceivingReceiptRepository receiptRepository, ReceivingReceiptLineRepository receiptLineRepository, WarehouseService warehouseService, ReceivingRequestFingerprint requestFingerprint, InventoryService inventoryService) {
        this.shipmentRepository = shipmentRepository;
        this.plannedLineRepository = plannedLineRepository;
        this.receiptRepository = receiptRepository;
        this.receiptLineRepository = receiptLineRepository;
        this.warehouseService = warehouseService;
        this.requestFingerprint = requestFingerprint;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public ReceivingReceiptCreationResult create(UUID tenantId, UUID warehouseId, UUID shipmentId, String idempotencyKey, CreateReceivingReceiptRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        InboundShipment shipment = shipmentRepository.findScopedForUpdate(shipmentId, tenantId, warehouseId)
                .orElseThrow(InboundShipmentNotFoundException::new);
        validateNoDuplicateRequestedLines(request.lines());
        String fingerprint = requestFingerprint.calculate(tenantId, shipmentId, request.lines());
        ReceivingReceipt existingReceipt = receiptRepository.findByTenantIdAndInboundShipmentIdAndIdempotencyKey(tenantId, shipmentId, idempotencyKey).orElse(null);
        if (existingReceipt != null) {
            if (!fingerprint.equals(existingReceipt.getRequestFingerprint())) throw new ReceivingIdempotencyKeyReusedWithDifferentRequestException();
            return new ReceivingReceiptCreationResult(toReceiptResponse(existingReceipt, receiptLineRepository.findByReceivingReceiptId(existingReceipt.getId()), cumulativeQuantities(tenantId, shipmentId)), true);
        }

        Map<UUID, InboundShipmentLine> plannedLines = validateRequestedLines(tenantId, shipmentId, request.lines());
        Map<UUID, Long> cumulative = cumulativeQuantities(tenantId, shipmentId);
        validateRemainingQuantities(request.lines(), plannedLines, cumulative);

        ReceivingReceipt receipt;
        try {
            receipt = receiptRepository.saveAndFlush(ReceivingReceipt.create(tenantId, shipment.getId(), idempotencyKey, fingerprint));
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyUniqueViolation(exception)) throw new ReceivingIdempotencyUniqueRaceException(fingerprint, exception);
            throw exception;
        }
        List<ReceivingReceiptLine> receiptLines = request.lines().stream()
                .map(line -> ReceivingReceiptLine.create(tenantId, shipment.getId(), receipt.getId(), line.inboundShipmentLineId(), line.receivedQuantity()))
                .toList();
        try {
            receiptLineRepository.saveAll(receiptLines);
            receiptLineRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLineViolation(exception)) throw new ReceivingDuplicateLineException();
            throw exception;
        }
        inventoryService.recordReceiving(tenantId, warehouseId, receiptLines.stream()
                .map(line -> new ReceivingInventoryMovement(line.getId(), plannedLines.get(line.getInboundShipmentLineId()).getSkuId(), line.getReceivedQuantity()))
                .toList());
        Map<UUID, Long> cumulativeAfter = cumulativeQuantities(tenantId, shipmentId);
        return new ReceivingReceiptCreationResult(toReceiptResponse(receipt, receiptLines, cumulativeAfter), false);
    }

    @Transactional(readOnly = true)
    public ReceivingReceiptResponse getById(UUID tenantId, UUID warehouseId, UUID shipmentId, UUID receiptId) {
        requireScopedShipment(tenantId, warehouseId, shipmentId);
        ReceivingReceipt receipt = receiptRepository.findByIdAndTenantIdAndInboundShipmentId(receiptId, tenantId, shipmentId)
                .orElseThrow(ReceivingReceiptNotFoundException::new);
        return toReceiptResponse(receipt, receiptLineRepository.findByReceivingReceiptId(receipt.getId()), cumulativeQuantities(tenantId, shipmentId));
    }

    @Transactional(readOnly = true)
    public ReceivingProgressResponse getProgress(UUID tenantId, UUID warehouseId, UUID shipmentId) {
        requireScopedShipment(tenantId, warehouseId, shipmentId);
        Map<UUID, Long> cumulative = cumulativeQuantities(tenantId, shipmentId);
        List<ReceivingProgressLineResponse> lines = plannedLineRepository.findByInboundShipmentId(shipmentId).stream()
                .map(line -> {
                    long received = cumulative.getOrDefault(line.getId(), 0L);
                    return new ReceivingProgressLineResponse(line.getId(), line.getSkuId(), line.getExpectedQuantity(), received, line.getExpectedQuantity() - received);
                })
                .toList();
        return new ReceivingProgressResponse(shipmentId, lines);
    }

    private InboundShipment requireScopedShipment(UUID tenantId, UUID warehouseId, UUID shipmentId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        return shipmentRepository.findByIdAndTenantIdAndWarehouseId(shipmentId, tenantId, warehouseId)
                .orElseThrow(InboundShipmentNotFoundException::new);
    }

    private void validateNoDuplicateRequestedLines(List<CreateReceivingReceiptLineRequest> requests) {
        Set<UUID> lineIds = new HashSet<>();
        for (CreateReceivingReceiptLineRequest request : requests) {
            if (!lineIds.add(request.inboundShipmentLineId())) throw new ReceivingDuplicateLineException();
        }
    }

    private Map<UUID, InboundShipmentLine> validateRequestedLines(UUID tenantId, UUID shipmentId, List<CreateReceivingReceiptLineRequest> requests) {
        Map<UUID, InboundShipmentLine> lines = new HashMap<>();
        for (CreateReceivingReceiptLineRequest request : requests) {
            InboundShipmentLine line = plannedLineRepository.findByIdAndTenantIdAndInboundShipmentId(request.inboundShipmentLineId(), tenantId, shipmentId)
                    .orElseThrow(ReceivingPlannedLineNotFoundException::new);
            lines.put(line.getId(), line);
        }
        return lines;
    }

    private void validateRemainingQuantities(List<CreateReceivingReceiptLineRequest> requests, Map<UUID, InboundShipmentLine> plannedLines, Map<UUID, Long> cumulative) {
        for (CreateReceivingReceiptLineRequest request : requests) {
            InboundShipmentLine line = plannedLines.get(request.inboundShipmentLineId());
            if (cumulative.getOrDefault(line.getId(), 0L) + request.receivedQuantity() > line.getExpectedQuantity()) {
                throw new ReceivingQuantityExceedsExpectedException();
            }
        }
    }

    private Map<UUID, Long> cumulativeQuantities(UUID tenantId, UUID shipmentId) {
        Map<UUID, Long> quantities = new HashMap<>();
        for (ReceivingReceiptLine line : receiptLineRepository.findByTenantIdAndInboundShipmentId(tenantId, shipmentId)) {
            quantities.merge(line.getInboundShipmentLineId(), line.getReceivedQuantity(), Long::sum);
        }
        return quantities;
    }

    private boolean isDuplicateLineViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation && violation.getKind() == ConstraintKind.UNIQUE && DUPLICATE_LINE_CONSTRAINT.equals(violation.getConstraintName())) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isIdempotencyUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation && violation.getKind() == ConstraintKind.UNIQUE && IDEMPOTENCY_CONSTRAINT.equals(violation.getConstraintName())) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private ReceivingReceiptResponse toReceiptResponse(ReceivingReceipt receipt, List<ReceivingReceiptLine> lines, Map<UUID, Long> cumulative) {
        List<ReceivingReceiptLineResponse> responses = lines.stream().map(line -> new ReceivingReceiptLineResponse(line.getId(), line.getInboundShipmentLineId(), line.getReceivedQuantity(), cumulative.getOrDefault(line.getInboundShipmentLineId(), 0L), line.getCreatedAt())).toList();
        return new ReceivingReceiptResponse(receipt.getId(), receipt.getTenantId(), receipt.getInboundShipmentId(), receipt.getCreatedAt(), responses);
    }
}
