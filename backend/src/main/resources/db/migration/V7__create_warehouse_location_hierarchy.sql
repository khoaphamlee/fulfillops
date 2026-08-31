CREATE TABLE fulfillops.warehouse_zones (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_warehouse_zones_warehouse_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_warehouse_zones_warehouse FOREIGN KEY (warehouse_id) REFERENCES fulfillops.warehouses(id),
    CONSTRAINT chk_warehouse_zones_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE fulfillops.warehouse_aisles (
    id UUID PRIMARY KEY,
    zone_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_warehouse_aisles_zone_code UNIQUE (zone_id, code),
    CONSTRAINT fk_warehouse_aisles_zone FOREIGN KEY (zone_id) REFERENCES fulfillops.warehouse_zones(id),
    CONSTRAINT chk_warehouse_aisles_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE fulfillops.warehouse_racks (
    id UUID PRIMARY KEY,
    aisle_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_warehouse_racks_aisle_code UNIQUE (aisle_id, code),
    CONSTRAINT fk_warehouse_racks_aisle FOREIGN KEY (aisle_id) REFERENCES fulfillops.warehouse_aisles(id),
    CONSTRAINT chk_warehouse_racks_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE fulfillops.warehouse_bins (
    id UUID PRIMARY KEY,
    rack_id UUID NOT NULL,
    code VARCHAR(63) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_warehouse_bins_rack_code UNIQUE (rack_id, code),
    CONSTRAINT fk_warehouse_bins_rack FOREIGN KEY (rack_id) REFERENCES fulfillops.warehouse_racks(id),
    CONSTRAINT chk_warehouse_bins_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);
