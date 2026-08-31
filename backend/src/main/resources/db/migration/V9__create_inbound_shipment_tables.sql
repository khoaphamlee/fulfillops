ALTER TABLE fulfillops.warehouses
    ADD CONSTRAINT uk_warehouses_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE fulfillops.skus
    ADD CONSTRAINT uk_skus_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE fulfillops.inbound_shipments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inbound_shipments_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_inbound_shipments_tenant FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants(id),
    CONSTRAINT fk_inbound_shipments_tenant_warehouse
        FOREIGN KEY (tenant_id, warehouse_id) REFERENCES fulfillops.warehouses(tenant_id, id)
);

CREATE TABLE fulfillops.inbound_shipment_lines (
    id UUID PRIMARY KEY,
    inbound_shipment_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    expected_quantity BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inbound_shipment_lines_shipment_sku UNIQUE (inbound_shipment_id, sku_id),
    CONSTRAINT fk_inbound_shipment_lines_tenant_shipment
        FOREIGN KEY (tenant_id, inbound_shipment_id)
        REFERENCES fulfillops.inbound_shipments(tenant_id, id),
    CONSTRAINT fk_inbound_shipment_lines_tenant_sku
        FOREIGN KEY (tenant_id, sku_id) REFERENCES fulfillops.skus(tenant_id, id),
    CONSTRAINT chk_inbound_shipment_lines_expected_quantity CHECK (expected_quantity > 0)
);
