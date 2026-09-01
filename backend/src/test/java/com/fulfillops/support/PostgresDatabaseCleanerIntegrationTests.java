package com.fulfillops.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostgresDatabaseCleanerIntegrationTests extends AbstractPostgresApplicationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void cleanerRemovesCommittedBusinessRowsAndPreservesFlywayHistory() throws Exception {
        execute("""
                INSERT INTO fulfillops.tenants (id, code, name, status, created_at, updated_at)
                VALUES (?, 'cleaner-test', 'Cleaner Test', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID());

        new PostgresDatabaseCleaner().clean(dataSource);

        assertEquals(0, count("SELECT COUNT(*) FROM fulfillops.tenants"));
        assertEquals(12, count("SELECT COUNT(*) FROM public.flyway_schema_history WHERE success = true"));
    }

    @Test
    void cleanerDoesNotSilentlySwallowDatabaseFailures() throws Exception {
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(new SQLException("intentional cleanup failure"));

        assertThrows(IllegalStateException.class, () -> new PostgresDatabaseCleaner().clean(failingDataSource));
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
