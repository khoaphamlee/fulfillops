package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class TenantMembershipRoleMigrationIntegrationTests {

    private static final String DATABASE = "fulfillops_role_migration_test";

    @BeforeEach
    void resetMigrationDatabase() {
        MigrationPostgres.resetDatabase(DATABASE);
    }

    @Test
    void v5BackfillsExistingV4MembershipAndEnforcesRoleConstraints() throws Exception {
        migrateToVersion("4");

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-31T00:00:00Z");
        insertV4Data(tenantId, userId, membershipId, createdAt);

        migrateToLatest();

        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT role, created_at, updated_at
                        FROM fulfillops.tenant_memberships
                        WHERE id = ?
                        """)) {
            statement.setObject(1, membershipId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("VIEWER", resultSet.getString("role"));
                assertEquals(resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant());
            }
        }

        assertFalse(columnIsNullable("role"));
        assertFalse(columnIsNullable("updated_at"));
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'chk_tenant_memberships_role'
                """));

        PSQLException exception = assertThrows(PSQLException.class, () -> executeUpdate("""
                UPDATE fulfillops.tenant_memberships
                SET role = 'OPERATOR'
                WHERE id = ?
                """, membershipId));
        assertEquals("chk_tenant_memberships_role", exception.getServerErrorMessage().getConstraint());
    }

    @Test
    void resetCreatesAnEmptyDatabaseForAnotherMigrationScenario() throws Exception {
        migrateToVersion("4");
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'tenant_memberships'
                """));

        MigrationPostgres.resetDatabase(DATABASE);

        assertEquals(0, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.schemata
                WHERE schema_name = 'fulfillops'
                """));
        migrateToVersion("4");
        assertEquals(1, queryForInt("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'fulfillops' AND table_name = 'tenant_memberships'
                """));
    }

    private void migrateToVersion(String version) {
        Flyway.configure()
                .dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password())
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void insertV4Data(UUID tenantId, UUID userId, UUID membershipId, Instant createdAt) throws SQLException {
        executeUpdate("""
                INSERT INTO fulfillops.tenants (id, code, name, status, created_at, updated_at)
                VALUES (?, 'migration-tenant', 'Migration Tenant', 'ACTIVE', ?, ?)
                """, tenantId, createdAt, createdAt);
        executeUpdate("""
                INSERT INTO fulfillops.users (id, email, display_name, created_at, updated_at)
                VALUES (?, 'migration@example.com', 'Migration User', ?, ?)
                """, userId, createdAt, createdAt);
        executeUpdate("""
                INSERT INTO fulfillops.tenant_memberships (id, tenant_id, user_id, created_at)
                VALUES (?, ?, ?, ?)
                """, membershipId, tenantId, userId, createdAt);
    }

    private boolean columnIsNullable(String columnName) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = 'fulfillops'
                          AND table_name = 'tenant_memberships'
                          AND column_name = ?
                        """)) {
            statement.setString(1, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return "YES".equals(resultSet.getString("is_nullable"));
            }
        }
    }

    private int queryForInt(String sql) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void executeUpdate(String sql, Object... values) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                Object value = values[index];
                if (value instanceof Instant instant) {
                    statement.setTimestamp(index + 1, Timestamp.from(instant));
                } else {
                    statement.setObject(index + 1, value);
                }
            }
            statement.executeUpdate();
        }
    }

    private Connection connection() throws SQLException {
        return java.sql.DriverManager.getConnection(
                MigrationPostgres.jdbcUrl(DATABASE), MigrationPostgres.username(), MigrationPostgres.password());
    }
}
