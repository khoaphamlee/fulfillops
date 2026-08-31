CREATE TABLE fulfillops.warehouses (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_warehouses_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_warehouses_tenant
        FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants (id),
    CONSTRAINT chk_warehouses_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);
