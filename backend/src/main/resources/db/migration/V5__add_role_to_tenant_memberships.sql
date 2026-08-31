ALTER TABLE fulfillops.tenant_memberships
    ADD COLUMN role VARCHAR(32);

UPDATE fulfillops.tenant_memberships
SET role = 'VIEWER'
WHERE role IS NULL;

ALTER TABLE fulfillops.tenant_memberships
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE fulfillops.tenant_memberships
    ADD CONSTRAINT chk_tenant_memberships_role CHECK (role IN ('ADMIN', 'VIEWER'));

ALTER TABLE fulfillops.tenant_memberships
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE fulfillops.tenant_memberships
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE fulfillops.tenant_memberships
    ALTER COLUMN updated_at SET NOT NULL;
