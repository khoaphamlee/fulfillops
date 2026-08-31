package com.fulfillops.warehouse.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "warehouse_aisles")
public class WarehouseAisle {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID zoneId;
    @Column(nullable = false, length = 63, updatable = false) private String code;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected WarehouseAisle() {}
    private WarehouseAisle(UUID id, UUID zoneId, String code) { this.id = id; this.zoneId = zoneId; this.code = code; }
    public static WarehouseAisle create(UUID zoneId, String code) { return new WarehouseAisle(UUID.randomUUID(), zoneId, code); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getZoneId() { return zoneId; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
}
