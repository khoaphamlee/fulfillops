package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.postgresql.util.PSQLException;

class ReceivingIdempotencyMigrationIntegrationTests {
    private static final String DATABASE = "fulfillops_receiving_idempotency_migration_test";

    @BeforeEach void resetMigrationDatabase() { MigrationPostgres.resetDatabase(DATABASE); }

    @Test void v11PreservesLegacyReceiptsAndEnforcesIdempotencyMetadataConstraints() throws Exception {
        migrateTo("10");
        UUID tenantId = UUID.randomUUID(); UUID warehouseId = UUID.randomUUID(); UUID shipmentId = UUID.randomUUID(); UUID legacyReceiptId = UUID.randomUUID();
        insertV10Receipt(tenantId, warehouseId, shipmentId, legacyReceiptId);
        migrateToLatest();

        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("SELECT idempotency_key, request_fingerprint FROM fulfillops.receiving_receipts WHERE id = ?")) {
            statement.setObject(1, legacyReceiptId); try (ResultSet result = statement.executeQuery()) { assertTrue(result.next()); assertNull(result.getString(1)); assertNull(result.getString(2)); }
        }
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_receiving_receipts_tenant_shipment_idempotency_key'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_idempotency_metadata_paired'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_idempotency_key_format'"));
        assertEquals(1, queryForInt("SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_receiving_receipts_request_fingerprint_format'"));

        UUID idempotentReceiptId = UUID.randomUUID(); insertReceipt(idempotentReceiptId, tenantId, shipmentId, "key-1", "a".repeat(64));
        PSQLException duplicate = assertThrows(PSQLException.class, () -> insertReceipt(UUID.randomUUID(), tenantId, shipmentId, "key-1", "b".repeat(64)));
        assertEquals("uk_receiving_receipts_tenant_shipment_idempotency_key", duplicate.getServerErrorMessage().getConstraint());
        PSQLException invalidFingerprint = assertThrows(PSQLException.class, () -> insertReceipt(UUID.randomUUID(), tenantId, shipmentId, "key-2", "invalid"));
        assertEquals("chk_receiving_receipts_request_fingerprint_format", invalidFingerprint.getServerErrorMessage().getConstraint());
    }

    private void migrateTo(String version) { Flyway.configure().dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()).locations("classpath:db/migration").target(version).load().migrate(); }
    private void migrateToLatest() { Flyway.configure().dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()).locations("classpath:db/migration").load().migrate(); }
    private void insertV10Receipt(UUID tenantId, UUID warehouseId, UUID shipmentId, UUID receiptId) throws SQLException {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        execute("INSERT INTO fulfillops.tenants (id, code, name, status, created_at, updated_at) VALUES (?, 'idempotency-migration', 'Migration', 'ACTIVE', ?, ?)", tenantId, now, now);
        execute("INSERT INTO fulfillops.warehouses (id, tenant_id, code, name, created_at) VALUES (?, ?, 'hcm-01', 'Warehouse', ?)", warehouseId, tenantId, now);
        execute("INSERT INTO fulfillops.inbound_shipments (id, tenant_id, warehouse_id, created_at) VALUES (?, ?, ?, ?)", shipmentId, tenantId, warehouseId, now);
        execute("INSERT INTO fulfillops.receiving_receipts (id, tenant_id, inbound_shipment_id, created_at) VALUES (?, ?, ?, ?)", receiptId, tenantId, shipmentId, now);
    }
    private void insertReceipt(UUID receiptId, UUID tenantId, UUID shipmentId, String key, String fingerprint) throws SQLException { execute("INSERT INTO fulfillops.receiving_receipts (id, tenant_id, inbound_shipment_id, idempotency_key, request_fingerprint, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", receiptId, tenantId, shipmentId, key, fingerprint); }
    private int queryForInt(String sql) throws SQLException { try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); } }
    private void execute(String sql, Object... values) throws SQLException { try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) { for (int index = 0; index < values.length; index++) { Object value = values[index]; if (value instanceof Instant instant) statement.setTimestamp(index + 1, Timestamp.from(instant)); else statement.setObject(index + 1, value); } statement.executeUpdate(); } }
    private Connection connection() throws SQLException { return java.sql.DriverManager.getConnection(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password()); }
}
