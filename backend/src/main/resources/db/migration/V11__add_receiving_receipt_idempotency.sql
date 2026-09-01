ALTER TABLE fulfillops.receiving_receipts
    ADD COLUMN idempotency_key VARCHAR(128),
    ADD COLUMN request_fingerprint VARCHAR(64),
    ADD CONSTRAINT chk_receiving_receipts_idempotency_metadata_paired
        CHECK (
            (idempotency_key IS NULL AND request_fingerprint IS NULL)
            OR (idempotency_key IS NOT NULL AND request_fingerprint IS NOT NULL)
        ),
    ADD CONSTRAINT chk_receiving_receipts_idempotency_key_format
        CHECK (idempotency_key IS NULL OR idempotency_key ~ '^[A-Za-z0-9._:-]+$'),
    ADD CONSTRAINT chk_receiving_receipts_request_fingerprint_format
        CHECK (request_fingerprint IS NULL OR request_fingerprint ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT uk_receiving_receipts_tenant_shipment_idempotency_key
        UNIQUE (tenant_id, inbound_shipment_id, idempotency_key);
