package com.fulfillops.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Test-only cleanup for committed HTTP and JDBC data in the application schema. */
public final class PostgresDatabaseCleaner {

    public void clean(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            List<String> tables = discoverTables(connection);
            if (tables.isEmpty()) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clean the fulfillops test schema.", exception);
        }
    }

    private List<String> discoverTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT quote_ident(schemaname) || '.' || quote_ident(tablename)
                        FROM pg_tables
                        WHERE schemaname = 'fulfillops'
                        ORDER BY tablename
                        """)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        return tables;
    }
}
