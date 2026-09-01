package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.AbstractPostgresApplicationIntegrationTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostgresFlywayIntegrationTests extends AbstractPostgresApplicationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void postgresIsReachableAndFlywayAppliesAllSchemaMigrations() throws Exception {
        assertEquals(1, queryForInt("SELECT 1"));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.schemata
                WHERE schema_name = 'fulfillops'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '1' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '2' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '3' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '4' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '5' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '6' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '7' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '8' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '9' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '10' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '11' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM public.flyway_schema_history
                WHERE version = '12' AND success = true
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'tenants'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'users'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'tenant_memberships'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'warehouses'
                """));
        assertEquals(4, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops'
                  AND table_name IN ('warehouse_zones', 'warehouse_aisles', 'warehouse_racks', 'warehouse_bins')
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'skus'
                """));
        assertEquals(2, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops'
                  AND table_name IN ('inbound_shipments', 'inbound_shipment_lines')
                """));
        assertEquals(2, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops'
                  AND table_name IN ('receiving_receipts', 'receiving_receipt_lines')
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'inventory_balances'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'fk_tenant_memberships_tenant'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'fk_tenant_memberships_user'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'chk_tenant_memberships_role'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'fk_warehouses_tenant'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'uk_warehouses_tenant_code'
                """));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'chk_warehouses_code_format'
                """));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_warehouse_zones_warehouse'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_warehouse_aisles_zone'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_warehouse_racks_aisle'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_warehouse_bins_rack'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_warehouse_zones_warehouse_code'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_warehouse_aisles_zone_code'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_warehouse_racks_aisle_code'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_warehouse_bins_rack_code'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_warehouse_zones_code_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_warehouse_aisles_code_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_warehouse_racks_code_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_warehouse_bins_code_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_skus_tenant'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_skus_tenant_code'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_skus_code_canonical_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_warehouses_tenant_id'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_skus_tenant_id'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_inbound_shipments_tenant_id'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inbound_shipments_tenant'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inbound_shipments_tenant_warehouse'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_inbound_shipment_lines_shipment_sku'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inbound_shipment_lines_tenant_shipment'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inbound_shipment_lines_tenant_sku'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_inbound_shipment_lines_expected_quantity'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_inbound_shipment_lines_tenant_shipment_id'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_receiving_receipts_tenant_shipment_id'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_receiving_receipts_tenant_shipment_idempotency_key'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_idempotency_metadata_paired'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_idempotency_key_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_request_fingerprint_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_inventory_balances_tenant_warehouse_sku'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inventory_balances_tenant'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inventory_balances_tenant_warehouse'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_inventory_balances_tenant_sku'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_inventory_balances_on_hand_quantity_non_negative'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_receiving_receipts_tenant_shipment'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_receiving_receipt_lines_receipt_planned_line'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_receiving_receipt_lines_tenant_shipment_receipt'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_receiving_receipt_lines_tenant_shipment_planned_line'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipt_lines_received_quantity'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'fulfillops' AND indexname = 'idx_receiving_receipt_lines_tenant_shipment_planned_line'"));

        MigrationInfo currentMigration = flyway.info().current();
        assertEquals("12", currentMigration.getVersion().getVersion());
        assertEquals(MigrationState.SUCCESS, currentMigration.getState());
    }

    private int queryForInt(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
