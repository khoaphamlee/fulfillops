package com.fulfillops.sku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.sku.domain.Sku;
import com.fulfillops.sku.infrastructure.SkuRepository;
import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SkuApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fulfillops_sku_test")
            .withUsername("fulfillops_test")
            .withPassword("fulfillops_test");

    @LocalServerPort
    private int port;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterEach
    void clearData() {
        skuRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void skuCanBeCreatedCanonicalizedPersistedAndReadInTenantScope() throws Exception {
        UUID tenantId = createTenant("sku-create");

        HttpResponse<String> createResponse = createSku(tenantId, "abc-001", "Black T-Shirt");
        assertEquals(201, createResponse.statusCode());
        UUID skuId = UUID.fromString(extract(ID_PATTERN, createResponse.body()));
        assertEquals("/api/v1/tenants/" + tenantId + "/skus/" + skuId,
                createResponse.headers().firstValue("Location").orElseThrow());
        assertTrue(createResponse.body().contains("\"tenantId\":\"" + tenantId + "\""));
        assertTrue(createResponse.body().contains("\"code\":\"ABC-001\""));
        assertTrue(createResponse.body().contains("\"name\":\"Black T-Shirt\""));

        Sku persisted = skuRepository.findById(skuId).orElseThrow();
        assertEquals(tenantId, persisted.getTenantId());
        assertEquals("ABC-001", persisted.getCode());
        assertTrue(persisted.getCreatedAt() != null);

        HttpResponse<String> getResponse = get("/api/v1/tenants/" + tenantId + "/skus/" + skuId);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"code\":\"ABC-001\""));
    }

    @Test
    void skuCodesAreCanonicalAndUniqueWithinTenantOnly() throws Exception {
        UUID tenantA = createTenant("sku-tenant-a");
        UUID tenantB = createTenant("sku-tenant-b");

        assertEquals(201, createSku(tenantA, "ABC-001", "Tenant A SKU").statusCode());
        assertEquals(201, createSku(tenantB, "abc-001", "Tenant B SKU").statusCode());
        assertStandardError(createSku(tenantA, "abc-001", "Duplicate A SKU"), 409, "SKU_CODE_CONFLICT",
                "/api/v1/tenants/" + tenantA + "/skus");
        assertEquals(201, createSku(tenantA, "abc_001", "Underscore SKU").statusCode());
    }

    @Test
    void skuGetDoesNotExposeAnotherTenantsResource() throws Exception {
        UUID tenantA = createTenant("sku-scope-a");
        UUID tenantB = createTenant("sku-scope-b");
        UUID skuId = UUID.fromString(extract(ID_PATTERN, createSku(tenantB, "ABC-001", "Tenant B SKU").body()));

        assertStandardError(get("/api/v1/tenants/" + tenantA + "/skus/" + skuId), 404, "SKU_NOT_FOUND",
                "/api/v1/tenants/" + tenantA + "/skus/" + skuId);
    }

    @Test
    void invalidSkuInputAndUnknownTenantUseExistingErrorContracts() throws Exception {
        HttpResponse<String> unknownTenant = createSku(UUID.randomUUID(), "ABC-001", "Unknown Tenant SKU");
        assertStandardError(unknownTenant, 404, "TENANT_NOT_FOUND", unknownTenant.request().uri().getPath());

        UUID tenantId = createTenant("sku-validation");
        assertStandardError(createSku(tenantId, " ABC-001 ", "Whitespace SKU"), 400, "VALIDATION_ERROR",
                "/api/v1/tenants/" + tenantId + "/skus");
        assertStandardError(createSku(tenantId, "ABC--001", "Bad Separator SKU"), 400, "VALIDATION_ERROR",
                "/api/v1/tenants/" + tenantId + "/skus");
    }

    @Test
    void databaseForeignKeyUniqueAndCanonicalCodeChecksRemainActive() throws Exception {
        UUID tenantId = createTenant("sku-database");
        executeUpdate("""
                INSERT INTO fulfillops.skus (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'ABC-001', 'SKU', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId);

        PSQLException duplicate = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.skus (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'ABC-001', 'Duplicate', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId));
        assertEquals("uk_skus_tenant_code", duplicate.getServerErrorMessage().getConstraint());

        PSQLException foreignKey = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.skus (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'ABC-002', 'Unknown Tenant', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), UUID.randomUUID()));
        assertEquals("fk_skus_tenant", foreignKey.getServerErrorMessage().getConstraint());

        PSQLException canonical = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.skus (id, tenant_id, code, name, created_at)
                VALUES (?, ?, 'abc-002', 'Noncanonical', CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId));
        assertEquals("chk_skus_code_canonical_format", canonical.getServerErrorMessage().getConstraint());
    }

    @Test
    void openApiDocumentsSkuRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/skus"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/skus/{skuId}"));
    }

    private UUID createTenant(String code) {
        return tenantRepository.saveAndFlush(Tenant.create(code, "Tenant " + code)).getId();
    }

    private HttpResponse<String> createSku(UUID tenantId, String code, String name) throws Exception {
        return postJson("/api/v1/tenants/" + tenantId + "/skus", """
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

    private void assertStandardError(HttpResponse<String> response, int status, String code, String path) {
        assertEquals(status, response.statusCode());
        assertTrue(response.body().contains("\"code\":\"" + code + "\""));
        assertTrue(response.body().contains("\"path\":\"" + path + "\""));
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(),
                extract(REQUEST_ID_PATTERN, response.body()));
        Instant.parse(extract(TIMESTAMP_PATTERN, response.body()));
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "Expected field was not present in response: " + body);
        return matcher.group(1);
    }
}
