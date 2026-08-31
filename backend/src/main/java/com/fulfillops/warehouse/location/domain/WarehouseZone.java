package com.fulfillops.warehouse.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "warehouse_zones")
public class WarehouseZone {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID warehouseId;
    @Column(nullable = false, length = 63, updatable = false) private String code;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected WarehouseZone() {}
    private WarehouseZone(UUID id, UUID warehouseId, String code) { this.id = id; this.warehouseId = warehouseId; this.code = code; }
    public static WarehouseZone create(UUID warehouseId, String code) { return new WarehouseZone(UUID.randomUUID(), warehouseId, code); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
}
