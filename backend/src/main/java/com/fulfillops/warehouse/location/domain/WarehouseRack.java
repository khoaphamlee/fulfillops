package com.fulfillops.warehouse.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "warehouse_racks")
public class WarehouseRack {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID aisleId;
    @Column(nullable = false, length = 63, updatable = false) private String code;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected WarehouseRack() {}
    private WarehouseRack(UUID id, UUID aisleId, String code) { this.id = id; this.aisleId = aisleId; this.code = code; }
    public static WarehouseRack create(UUID aisleId, String code) { return new WarehouseRack(UUID.randomUUID(), aisleId, code); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getAisleId() { return aisleId; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
}
