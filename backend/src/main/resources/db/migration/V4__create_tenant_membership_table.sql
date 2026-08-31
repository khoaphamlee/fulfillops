CREATE TABLE fulfillops.tenant_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tenant_memberships_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT fk_tenant_memberships_tenant
        FOREIGN KEY (tenant_id) REFERENCES fulfillops.tenants (id),
    CONSTRAINT fk_tenant_memberships_user
        FOREIGN KEY (user_id) REFERENCES fulfillops.users (id)
);
