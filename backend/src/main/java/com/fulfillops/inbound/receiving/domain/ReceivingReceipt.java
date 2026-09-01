package com.fulfillops.inbound.receiving.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "receiving_receipts")
public class ReceivingReceipt {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID inboundShipmentId;
    @Column(updatable = false, length = 128) private String idempotencyKey;
    @Column(updatable = false, length = 64) private String requestFingerprint;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected ReceivingReceipt() {}
    private ReceivingReceipt(UUID id, UUID tenantId, UUID inboundShipmentId, String idempotencyKey, String requestFingerprint) { this.id = id; this.tenantId = tenantId; this.inboundShipmentId = inboundShipmentId; this.idempotencyKey = idempotencyKey; this.requestFingerprint = requestFingerprint; }
    public static ReceivingReceipt create(UUID tenantId, UUID inboundShipmentId, String idempotencyKey, String requestFingerprint) {
        if (idempotencyKey == null || requestFingerprint == null) throw new IllegalArgumentException("New receiving receipts require idempotency metadata.");
        return new ReceivingReceipt(UUID.randomUUID(), tenantId, inboundShipmentId, idempotencyKey, requestFingerprint);
    }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getInboundShipmentId() { return inboundShipmentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public Instant getCreatedAt() { return createdAt; }
}
