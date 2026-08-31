package com.fulfillops.inbound.receiving.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "receiving_receipt_lines")
public class ReceivingReceiptLine {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID inboundShipmentId;
    @Column(nullable = false, updatable = false) private UUID receivingReceiptId;
    @Column(nullable = false, updatable = false) private UUID inboundShipmentLineId;
    @Column(nullable = false, updatable = false) private long receivedQuantity;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected ReceivingReceiptLine() {}
    private ReceivingReceiptLine(UUID id, UUID tenantId, UUID inboundShipmentId, UUID receivingReceiptId, UUID inboundShipmentLineId, long receivedQuantity) { this.id = id; this.tenantId = tenantId; this.inboundShipmentId = inboundShipmentId; this.receivingReceiptId = receivingReceiptId; this.inboundShipmentLineId = inboundShipmentLineId; this.receivedQuantity = receivedQuantity; }
    public static ReceivingReceiptLine create(UUID tenantId, UUID inboundShipmentId, UUID receivingReceiptId, UUID inboundShipmentLineId, long receivedQuantity) { return new ReceivingReceiptLine(UUID.randomUUID(), tenantId, inboundShipmentId, receivingReceiptId, inboundShipmentLineId, receivedQuantity); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getInboundShipmentId() { return inboundShipmentId; }
    public UUID getReceivingReceiptId() { return receivingReceiptId; }
    public UUID getInboundShipmentLineId() { return inboundShipmentLineId; }
    public long getReceivedQuantity() { return receivedQuantity; }
    public Instant getCreatedAt() { return createdAt; }
}
