package com.fulfillops.inventory.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "inventory_ledger_entries")
public class InventoryLedgerEntry {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false) private UUID warehouseId;
    @Column(nullable = false, updatable = false) private UUID skuId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 32) private InventoryMovementType movementType;
    @Column(nullable = false, updatable = false) private long quantityDelta;
    @Column(nullable = false, updatable = false) private UUID receivingReceiptLineId;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected InventoryLedgerEntry() {}

    private InventoryLedgerEntry(UUID tenantId, UUID warehouseId, UUID skuId, UUID receivingReceiptLineId, long quantityDelta, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
        this.skuId = skuId;
        this.movementType = InventoryMovementType.RECEIVING;
        this.quantityDelta = quantityDelta;
        this.receivingReceiptLineId = receivingReceiptLineId;
        this.createdAt = createdAt;
    }

    public static InventoryLedgerEntry receiving(UUID tenantId, UUID warehouseId, UUID skuId, UUID receivingReceiptLineId, long quantityDelta, Instant createdAt) {
        if (tenantId == null || warehouseId == null || skuId == null || receivingReceiptLineId == null || createdAt == null || quantityDelta <= 0) {
            throw new IllegalArgumentException("Receiving ledger entries require scoped provenance and a positive quantity.");
        }
        return new InventoryLedgerEntry(tenantId, warehouseId, skuId, receivingReceiptLineId, quantityDelta, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getSkuId() { return skuId; }
    public InventoryMovementType getMovementType() { return movementType; }
    public long getQuantityDelta() { return quantityDelta; }
    public UUID getReceivingReceiptLineId() { return receivingReceiptLineId; }
    public Instant getCreatedAt() { return createdAt; }
}
