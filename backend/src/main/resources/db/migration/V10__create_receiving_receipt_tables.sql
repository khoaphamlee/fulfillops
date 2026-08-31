ALTER TABLE fulfillops.inbound_shipment_lines
    ADD CONSTRAINT uk_inbound_shipment_lines_tenant_shipment_id
    UNIQUE (tenant_id, inbound_shipment_id, id);

CREATE TABLE fulfillops.receiving_receipts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    inbound_shipment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_receiving_receipts_tenant_shipment_id UNIQUE (tenant_id, inbound_shipment_id, id),
    CONSTRAINT fk_receiving_receipts_tenant_shipment
        FOREIGN KEY (tenant_id, inbound_shipment_id)
        REFERENCES fulfillops.inbound_shipments(tenant_id, id)
);

CREATE TABLE fulfillops.receiving_receipt_lines (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    inbound_shipment_id UUID NOT NULL,
    receiving_receipt_id UUID NOT NULL,
    inbound_shipment_line_id UUID NOT NULL,
    received_quantity BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_receiving_receipt_lines_receipt_planned_line
        UNIQUE (receiving_receipt_id, inbound_shipment_line_id),
    CONSTRAINT fk_receiving_receipt_lines_tenant_shipment_receipt
        FOREIGN KEY (tenant_id, inbound_shipment_id, receiving_receipt_id)
        REFERENCES fulfillops.receiving_receipts(tenant_id, inbound_shipment_id, id),
    CONSTRAINT fk_receiving_receipt_lines_tenant_shipment_planned_line
        FOREIGN KEY (tenant_id, inbound_shipment_id, inbound_shipment_line_id)
        REFERENCES fulfillops.inbound_shipment_lines(tenant_id, inbound_shipment_id, id),
    CONSTRAINT chk_receiving_receipt_lines_received_quantity CHECK (received_quantity > 0)
);

CREATE INDEX idx_receiving_receipt_lines_tenant_shipment_planned_line
    ON fulfillops.receiving_receipt_lines (tenant_id, inbound_shipment_id, inbound_shipment_line_id);
