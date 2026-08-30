package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.flyway.enabled=true",
        "management.health.db.enabled=true"
})
class PostgresFlywayIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fulfillops_test")
            .withUsername("fulfillops_test")
            .withPassword("fulfillops_test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void postgresIsReachableAndFlywayAppliesTheInitialSchemaMigration() throws Exception {
        assertTrue(POSTGRES.isRunning());
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

        MigrationInfo currentMigration = flyway.info().current();
        assertEquals("1", currentMigration.getVersion().getVersion());
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
