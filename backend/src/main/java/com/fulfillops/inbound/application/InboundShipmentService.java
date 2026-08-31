package com.fulfillops.inbound.application;

import com.fulfillops.inbound.domain.InboundShipment;
import com.fulfillops.inbound.domain.InboundShipmentLine;
import com.fulfillops.inbound.infrastructure.InboundShipmentLineRepository;
import com.fulfillops.inbound.infrastructure.InboundShipmentRepository;
import com.fulfillops.inbound.presentation.CreateInboundShipmentLineRequest;
import com.fulfillops.inbound.presentation.CreateInboundShipmentRequest;
import com.fulfillops.inbound.presentation.InboundShipmentLineResponse;
import com.fulfillops.inbound.presentation.InboundShipmentResponse;
import com.fulfillops.sku.application.SkuService;
import com.fulfillops.warehouse.application.WarehouseService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundShipmentService {

    private static final String DUPLICATE_SKU_LINE_CONSTRAINT = "uk_inbound_shipment_lines_shipment_sku";
    private final InboundShipmentRepository shipmentRepository;
    private final InboundShipmentLineRepository lineRepository;
    private final WarehouseService warehouseService;
    private final SkuService skuService;

    public InboundShipmentService(
            InboundShipmentRepository shipmentRepository,
            InboundShipmentLineRepository lineRepository,
            WarehouseService warehouseService,
            SkuService skuService) {
        this.shipmentRepository = shipmentRepository;
        this.lineRepository = lineRepository;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
    }

    @Transactional
    public InboundShipmentResponse create(UUID tenantId, UUID warehouseId, CreateInboundShipmentRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        validateSkuReferences(tenantId, request.lines());

        InboundShipment shipment = shipmentRepository.saveAndFlush(InboundShipment.create(tenantId, warehouseId));
        List<InboundShipmentLine> lines = request.lines().stream()
                .map(line -> InboundShipmentLine.create(
                        shipment.getId(), tenantId, line.skuId(), line.expectedQuantity()))
                .toList();
        try {
            lineRepository.saveAll(lines);
            lineRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateSkuLineViolation(exception)) {
                throw new InboundShipmentDuplicateSkuLineException();
            }
            throw exception;
        }
        return toResponse(shipment, lines);
    }

    @Transactional(readOnly = true)
    public InboundShipmentResponse getById(UUID tenantId, UUID warehouseId, UUID shipmentId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        InboundShipment shipment = shipmentRepository.findByIdAndTenantIdAndWarehouseId(shipmentId, tenantId, warehouseId)
                .orElseThrow(InboundShipmentNotFoundException::new);
        return toResponse(shipment, lineRepository.findByInboundShipmentId(shipment.getId()));
    }

    private void validateSkuReferences(UUID tenantId, List<CreateInboundShipmentLineRequest> lines) {
        Set<UUID> skuIds = new HashSet<>();
        for (CreateInboundShipmentLineRequest line : lines) {
            if (!skuIds.add(line.skuId())) {
                throw new InboundShipmentDuplicateSkuLineException();
            }
            skuService.requireExistingSku(tenantId, line.skuId());
        }
    }

    private boolean isDuplicateSkuLineViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getKind() == ConstraintKind.UNIQUE
                    && DUPLICATE_SKU_LINE_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private InboundShipmentResponse toResponse(InboundShipment shipment, List<InboundShipmentLine> lines) {
        List<InboundShipmentLineResponse> responses = lines.stream()
                .map(line -> new InboundShipmentLineResponse(
                        line.getId(), line.getSkuId(), line.getExpectedQuantity(), line.getCreatedAt()))
                .toList();
        return new InboundShipmentResponse(
                shipment.getId(), shipment.getTenantId(), shipment.getWarehouseId(), shipment.getCreatedAt(), responses);
    }
}
