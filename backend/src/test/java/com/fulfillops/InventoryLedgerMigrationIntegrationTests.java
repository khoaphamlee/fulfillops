package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fulfillops.support.MigrationPostgres;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryLedgerMigrationIntegrationTests {
    private static final String DATABASE = "fulfillops_inventory_ledger_migration_test";

    @BeforeEach void resetMigrationDatabase() { MigrationPostgres.resetDatabase(DATABASE); }

    @Test void v13PreservesExistingBalanceWithoutFabricatingLedgerHistory() throws Exception {
        migrateTo("12");
        UUID tenant = UUID.randomUUID(); UUID warehouse = UUID.randomUUID(); UUID sku = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-09-01T00:00:00Z");
        execute("INSERT INTO fulfillops.tenants (id, code, name, status, created_at, updated_at) VALUES (?, 'ledger-migration', 'Ledger migration', 'ACTIVE', ?, ?)", tenant, timestamp, timestamp);
        execute("INSERT INTO fulfillops.warehouses (id, tenant_id, code, name, created_at) VALUES (?, ?, 'hcm-01', 'Warehouse', ?)", warehouse, tenant, timestamp);
        execute("INSERT INTO fulfillops.skus (id, tenant_id, code, name, created_at) VALUES (?, ?, 'LEDGER-001', 'SKU', ?)", sku, tenant, timestamp);
        execute("INSERT INTO fulfillops.inventory_balances (id, tenant_id, warehouse_id, sku_id, on_hand_quantity, created_at, updated_at) VALUES (?, ?, ?, ?, 50, ?, ?)", UUID.randomUUID(), tenant, warehouse, sku, timestamp, timestamp);

        migrateToLatest();

        assertEquals(50, queryForLong("SELECT on_hand_quantity FROM fulfillops.inventory_balances WHERE tenant_id = '" + tenant + "'"));
        assertEquals(0, queryForLong("SELECT COUNT(*) FROM fulfillops.inventory_ledger_entries"));
        assertEquals(1, queryForLong("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_receiving_receipt_lines_tenant_id'"));
        assertEquals(1, queryForLong("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_inventory_ledger_entries_tenant_receiving_receipt_line'"));
    }

    private void migrateTo(String version) { Flyway.configure().dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()).locations("classpath:db/migration").target(version).load().migrate(); }
    private void migrateToLatest() { Flyway.configure().dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()).locations("classpath:db/migration").load().migrate(); }
    private long queryForLong(String sql) throws SQLException { try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); } }
    private void execute(String sql, Object... values) throws SQLException { try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) { for (int index = 0; index < values.length; index++) { Object value = values[index]; if (value instanceof Instant instant) statement.setTimestamp(index + 1, Timestamp.from(instant)); else statement.setObject(index + 1, value); } statement.executeUpdate(); } }
    private Connection connection() throws SQLException { return java.sql.DriverManager.getConnection(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()); }
}
