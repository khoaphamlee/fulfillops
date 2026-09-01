ALTER TABLE fulfillops.receiving_receipt_lines
    ADD CONSTRAINT uk_receiving_receipt_lines_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE fulfillops.inventory_ledger_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta BIGINT NOT NULL,
    receiving_receipt_line_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_inventory_ledger_entries_tenant
        FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants(id),
    CONSTRAINT fk_inventory_ledger_entries_tenant_warehouse
        FOREIGN KEY (tenant_id, warehouse_id)
        REFERENCES fulfillops.warehouses(tenant_id, id),
    CONSTRAINT fk_inventory_ledger_entries_tenant_sku
        FOREIGN KEY (tenant_id, sku_id)
        REFERENCES fulfillops.skus(tenant_id, id),
    CONSTRAINT fk_inventory_ledger_entries_tenant_receiving_receipt_line
        FOREIGN KEY (tenant_id, receiving_receipt_line_id)
        REFERENCES fulfillops.receiving_receipt_lines(tenant_id, id),
    CONSTRAINT uk_inventory_ledger_entries_tenant_receiving_receipt_line
        UNIQUE (tenant_id, receiving_receipt_line_id),
    CONSTRAINT chk_inventory_ledger_entries_movement_type
        CHECK (movement_type IN ('RECEIVING')),
    CONSTRAINT chk_inventory_ledger_entries_receiving_quantity_delta
        CHECK (quantity_delta > 0)
);
