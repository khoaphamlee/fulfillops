package com.fulfillops.inbound.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "inbound_shipment_lines")
public class InboundShipmentLine {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID inboundShipmentId;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID skuId;
    @Column(nullable = false, updatable = false) private long expectedQuantity;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected InboundShipmentLine() {}
    private InboundShipmentLine(UUID id, UUID inboundShipmentId, UUID tenantId, UUID skuId, long expectedQuantity) { this.id = id; this.inboundShipmentId = inboundShipmentId; this.tenantId = tenantId; this.skuId = skuId; this.expectedQuantity = expectedQuantity; }
    public static InboundShipmentLine create(UUID inboundShipmentId, UUID tenantId, UUID skuId, long expectedQuantity) { return new InboundShipmentLine(UUID.randomUUID(), inboundShipmentId, tenantId, skuId, expectedQuantity); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getInboundShipmentId() { return inboundShipmentId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSkuId() { return skuId; }
    public long getExpectedQuantity() { return expectedQuantity; }
    public Instant getCreatedAt() { return createdAt; }
}
