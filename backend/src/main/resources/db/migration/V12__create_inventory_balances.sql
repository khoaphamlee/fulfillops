CREATE TABLE fulfillops.inventory_balances (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    on_hand_quantity BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inventory_balances_tenant_warehouse_sku
        UNIQUE (tenant_id, warehouse_id, sku_id),
    CONSTRAINT fk_inventory_balances_tenant
        FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants(id),
    CONSTRAINT fk_inventory_balances_tenant_warehouse
        FOREIGN KEY (tenant_id, warehouse_id)
        REFERENCES fulfillops.warehouses(tenant_id, id),
    CONSTRAINT fk_inventory_balances_tenant_sku
        FOREIGN KEY (tenant_id, sku_id)
        REFERENCES fulfillops.skus(tenant_id, id),
    CONSTRAINT chk_inventory_balances_on_hand_quantity_non_negative
        CHECK (on_hand_quantity >= 0)
);
