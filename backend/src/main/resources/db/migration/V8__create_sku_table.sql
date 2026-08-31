CREATE TABLE fulfillops.skus (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_skus_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_skus_tenant FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants(id),
    CONSTRAINT chk_skus_code_canonical_format
        CHECK (code = upper(code) AND code ~ '^[A-Z0-9]+([-_][A-Z0-9]+)*$')
);
