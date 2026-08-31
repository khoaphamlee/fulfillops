package com.fulfillops.inbound.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "inbound_shipments")
public class InboundShipment {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID warehouseId;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected InboundShipment() {}
    private InboundShipment(UUID id, UUID tenantId, UUID warehouseId) { this.id = id; this.tenantId = tenantId; this.warehouseId = warehouseId; }
    public static InboundShipment create(UUID tenantId, UUID warehouseId) { return new InboundShipment(UUID.randomUUID(), tenantId, warehouseId); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getWarehouseId() { return warehouseId; }
    public Instant getCreatedAt() { return createdAt; }
}
