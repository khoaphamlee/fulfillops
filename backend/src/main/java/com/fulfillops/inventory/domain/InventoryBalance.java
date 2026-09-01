package com.fulfillops.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "inventory_balances")
public class InventoryBalance {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID warehouseId;
    @Column(nullable = false, updatable = false) private UUID skuId;
    @Column(nullable = false) private long onHandQuantity;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected InventoryBalance() {}
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getSkuId() { return skuId; }
    public long getOnHandQuantity() { return onHandQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
