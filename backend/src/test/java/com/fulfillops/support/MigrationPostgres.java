package com.fulfillops.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL server dedicated to migration-evolution tests. Each scenario receives a freshly
 * recreated database; the singleton server alone is not considered isolation.
 */
public final class MigrationPostgres {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fulfillops_migration_server")
            .withUsername("fulfillops_test")
            .withPassword("fulfillops_test");

    private MigrationPostgres() {
    }

    public static synchronized void resetDatabase(String databaseName) {
        validateDatabaseName(databaseName);
        try (Connection connection = adminConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
            statement.execute("CREATE DATABASE " + databaseName);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not reset isolated migration test database.", exception);
        }
    }

    public static String jdbcUrl(String databaseName) {
        validateDatabaseName(databaseName);
        PostgreSQLContainer container = container();
        return "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/" + databaseName;
    }

    public static String username() {
        return container().getUsername();
    }

    public static String password() {
        return container().getPassword();
    }

    private static Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl("postgres"), username(), password());
    }

    private static synchronized PostgreSQLContainer container() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        return POSTGRES;
    }

    private static void validateDatabaseName(String databaseName) {
        if (!databaseName.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Migration test database name is invalid.");
        }
    }
}
