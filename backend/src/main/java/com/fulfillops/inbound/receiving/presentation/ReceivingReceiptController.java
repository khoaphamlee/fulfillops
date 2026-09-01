package com.fulfillops.inbound.receiving.presentation;

import com.fulfillops.inbound.receiving.application.ReceivingCommandCoordinator;
import com.fulfillops.inbound.receiving.application.ReceivingReceiptCreationResult;
import com.fulfillops.inbound.receiving.application.ReceivingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}")
public class ReceivingReceiptController {
    private final ReceivingCommandCoordinator receivingCommandCoordinator;
    private final ReceivingService receivingService;
    private final ReceivingIdempotencyKeyValidator idempotencyKeyValidator;
    public ReceivingReceiptController(ReceivingCommandCoordinator receivingCommandCoordinator, ReceivingService receivingService, ReceivingIdempotencyKeyValidator idempotencyKeyValidator) { this.receivingCommandCoordinator = receivingCommandCoordinator; this.receivingService = receivingService; this.idempotencyKeyValidator = idempotencyKeyValidator; }
    @PostMapping("/receipts")
    public ResponseEntity<ReceivingReceiptResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID shipmentId, @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Required Receiving-specific idempotency key.") @RequestHeader(value = "Idempotency-Key", required = false) String rawIdempotencyKey, @Valid @RequestBody CreateReceivingReceiptRequest request) {
        ReceivingReceiptCreationResult result = receivingCommandCoordinator.create(tenantId, warehouseId, shipmentId, idempotencyKeyValidator.validate(rawIdempotencyKey), request);
        ReceivingReceiptResponse response = result.response();
        URI location = URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/inbound-shipments/" + shipmentId + "/receipts/" + response.id());
        return result.replayed() ? ResponseEntity.ok().location(location).body(response) : ResponseEntity.created(location).body(response);
    }
    @GetMapping("/receipts/{receiptId}")
    public ReceivingReceiptResponse getById(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID shipmentId, @PathVariable UUID receiptId) {
        return receivingService.getById(tenantId, warehouseId, shipmentId, receiptId);
    }
    @GetMapping("/receiving-progress")
    public ReceivingProgressResponse getProgress(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID shipmentId) {
        return receivingService.getProgress(tenantId, warehouseId, shipmentId);
    }
}
