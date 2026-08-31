package com.fulfillops.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "warehouses")
public class Warehouse {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 63, updatable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Warehouse() {
    }

    private Warehouse(UUID id, UUID tenantId, String code, String name) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
    }

    public static Warehouse create(UUID tenantId, String code, String name) {
        return new Warehouse(UUID.randomUUID(), tenantId, code, name);
    }

    @PrePersist
    void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
