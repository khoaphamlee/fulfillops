package com.fulfillops.inbound.receiving.presentation;

import com.fulfillops.inbound.receiving.application.ReceivingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}")
public class ReceivingReceiptController {
    private final ReceivingService receivingService;
    public ReceivingReceiptController(ReceivingService receivingService) { this.receivingService = receivingService; }
    @PostMapping("/receipts")
    public ResponseEntity<ReceivingReceiptResponse> create(@PathVariable UUID tenantId, @PathVariable UUID warehouseId, @PathVariable UUID shipmentId, @Valid @RequestBody CreateReceivingReceiptRequest request) {
        ReceivingReceiptResponse response = receivingService.create(tenantId, warehouseId, shipmentId, request);
        URI location = URI.create("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/inbound-shipments/" + shipmentId + "/receipts/" + response.id());
        return ResponseEntity.created(location).body(response);
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
