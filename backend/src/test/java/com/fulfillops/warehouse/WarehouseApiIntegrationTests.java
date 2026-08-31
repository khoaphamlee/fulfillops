package com.fulfillops.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.AbstractPostgresApplicationIntegrationTest;
import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
import com.fulfillops.warehouse.domain.Warehouse;
import com.fulfillops.warehouse.infrastructure.WarehouseRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class WarehouseApiIntegrationTests extends AbstractPostgresApplicationIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");

    @LocalServerPort
    private int port;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void warehouseCanBeCreatedPersistedAndReadInItsTenantScope() throws Exception {
        UUID tenantId = createTenant("warehouse-tenant");

        HttpResponse<String> createResponse = createWarehouse(tenantId, "hcm-01", "Ho Chi Minh DC");
        assertEquals(201, createResponse.statusCode());
        UUID warehouseId = UUID.fromString(extract(ID_PATTERN, createResponse.body()));
        assertEquals("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId,
                createResponse.headers().firstValue("Location").orElseThrow());
        assertTrue(createResponse.body().contains("\"tenantId\":\"" + tenantId + "\""));
        assertTrue(createResponse.body().contains("\"code\":\"hcm-01\""));
        assertTrue(createResponse.body().contains("\"name\":\"Ho Chi Minh DC\""));

        Warehouse persistedWarehouse = warehouseRepository.findById(warehouseId).orElseThrow();
        assertEquals(tenantId, persistedWarehouse.getTenantId());
        assertEquals("hcm-01", persistedWarehouse.getCode());

        HttpResponse<String> getResponse = get("/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"id\":\"" + warehouseId + "\""));
    }

    @Test
    void warehouseCodesAreUniqueWithinTenantOnly() throws Exception {
        UUID tenantA = createTenant("warehouse-tenant-a");
        UUID tenantB = createTenant("warehouse-tenant-b");

        assertEquals(201, createWarehouse(tenantA, "hcm-01", "Tenant A DC").statusCode());
        assertEquals(201, createWarehouse(tenantB, "hcm-01", "Tenant B DC").statusCode());

        HttpResponse<String> duplicateResponse = createWarehouse(tenantA, "hcm-01", "Duplicate A DC");
        assertStandardError(duplicateResponse, 409, "WAREHOUSE_CODE_CONFLICT",
                "/api/v1/tenants/" + tenantA + "/warehouses");
    }

    @Test
    void warehouseGetDoesNotExposeAnotherTenantsResource() throws Exception {
        UUID tenantA = createTenant("warehouse-scope-a");
        UUID tenantB = createTenant("warehouse-scope-b");
        UUID warehouseId = UUID.fromString(extract(ID_PATTERN,
                createWarehouse(tenantB, "hcm-01", "Tenant B DC").body()));

        HttpResponse<String> response = get("/api/v1/tenants/" + tenantA + "/warehouses/" + warehouseId);
        assertStandardError(response, 404, "WAREHOUSE_NOT_FOUND",
                "/api/v1/tenants/" + tenantA + "/warehouses/" + warehouseId);
    }

    @Test
    void unknownTenantAndInvalidRequestUseExistingErrorContracts() throws Exception {
        HttpResponse<String> unknownTenantResponse = createWarehouse(UUID.randomUUID(), "hcm-01", "Unknown Tenant DC");
        assertStandardError(unknownTenantResponse, 404, "TENANT_NOT_FOUND",
                unknownTenantResponse.request().uri().getPath());

        UUID tenantId = createTenant("warehouse-validation");
        HttpResponse<String> validationResponse = createWarehouse(tenantId, "INVALID_CODE", "");
        assertStandardError(validationResponse, 400, "VALIDATION_ERROR",
                "/api/v1/tenants/" + tenantId + "/warehouses");
        assertTrue(validationResponse.body().contains("\"field\":\"code\""));
        assertTrue(validationResponse.body().contains("\"field\":\"name\""));
    }

    @Test
    void databaseForeignKeyAndTenantScopedUniqueConstraintRemainActive() throws Exception {
        UUID tenantId = createTenant("warehouse-foreign-key");
        executeUpdate("""
                INSERT INTO fulfillops.warehouses (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'hcm-01', 'Ho Chi Minh DC', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId);

        PSQLException duplicate = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.warehouses (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'hcm-01', 'Duplicate DC', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId));
        assertEquals("uk_warehouses_tenant_code", duplicate.getServerErrorMessage().getConstraint());

        PSQLException foreignKey = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.warehouses (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'hcm-02', 'Unknown Tenant DC', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), UUID.randomUUID()));
        assertEquals("fk_warehouses_tenant", foreignKey.getServerErrorMessage().getConstraint());
    }

    @Test
    void openApiDocumentsWarehouseRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}"));
    }

    private UUID createTenant(String code) {
        return tenantRepository.saveAndFlush(Tenant.create(code, "Tenant " + code)).getId();
    }

    private HttpResponse<String> createWarehouse(UUID tenantId, String code, String name) throws Exception {
        return postJson("/api/v1/tenants/" + tenantId + "/warehouses", """
                {"code":"%s","name":"%s"}
                """.formatted(code, name));
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void executeUpdate(String sql, Object... values) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
        }
    }

    private void assertStandardError(
            HttpResponse<String> response,
            int expectedStatus,
            String expectedCode,
            String expectedPath) {
        assertEquals(expectedStatus, response.statusCode());
        assertTrue(response.body().contains("\"code\":\"" + expectedCode + "\""));
        assertTrue(response.body().contains("\"status\":" + expectedStatus));
        assertTrue(response.body().contains("\"path\":\"" + expectedPath + "\""));
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(),
                extract(REQUEST_ID_PATTERN, response.body()));
        if (!"VALIDATION_ERROR".equals(expectedCode)) {
            assertTrue(response.body().contains("\"fieldErrors\":[]"));
        }
        Instant.parse(extract(TIMESTAMP_PATTERN, response.body()));
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "Expected field was not present in response: " + body);
        return matcher.group(1);
    }
}
