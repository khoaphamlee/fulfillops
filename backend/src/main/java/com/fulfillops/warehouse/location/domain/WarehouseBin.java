package com.fulfillops.warehouse.location.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "warehouse_bins")
public class WarehouseBin {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID rackId;
    @Column(nullable = false, length = 63, updatable = false) private String code;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected WarehouseBin() {}
    private WarehouseBin(UUID id, UUID rackId, String code) { this.id = id; this.rackId = rackId; this.code = code; }
    public static WarehouseBin create(UUID rackId, String code) { return new WarehouseBin(UUID.randomUUID(), rackId, code); }
    @PrePersist void initializeCreatedAt() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getRackId() { return rackId; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
}
