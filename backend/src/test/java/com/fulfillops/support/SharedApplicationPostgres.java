package com.fulfillops.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One latest-schema PostgreSQL server per Maven test JVM. It is deliberately started without
 * JUnit's per-class container lifecycle so cached Spring contexts keep a live datasource.
 */
public final class SharedApplicationPostgres {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fulfillops_application_test")
            .withUsername("fulfillops_test")
            .withPassword("fulfillops_test");

    private SharedApplicationPostgres() {
    }

    public static String jdbcUrl() {
        return container().getJdbcUrl();
    }

    public static String username() {
        return container().getUsername();
    }

    public static String password() {
        return container().getPassword();
    }

    public static boolean isRunning() {
        return POSTGRES.isRunning();
    }

    private static synchronized PostgreSQLContainer container() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        return POSTGRES;
    }
}
