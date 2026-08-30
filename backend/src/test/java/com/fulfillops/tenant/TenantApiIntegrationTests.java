package com.fulfillops.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
class TenantApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fulfillops_tenant_test")
            .withUsername("fulfillops_test")
            .withPassword("fulfillops_test");

    @LocalServerPort
    private int port;

    @Autowired
    private TenantRepository tenantRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterEach
    void clearTenantData() {
        tenantRepository.deleteAll();
    }

    @Test
    void postCreatesTenantThatCanBeReadBackAndFetchedById() throws Exception {
        HttpResponse<String> createResponse = postJson("""
                {"code":"acme-logistics","name":"Acme Logistics"}
                """);

        assertEquals(201, createResponse.statusCode());
        UUID tenantId = UUID.fromString(extract(ID_PATTERN, createResponse.body()));
        assertEquals("/api/v1/tenants/" + tenantId,
                createResponse.headers().firstValue("Location").orElseThrow());
        assertTrue(createResponse.body().contains("\"status\":\"ACTIVE\""));

        Tenant persistedTenant = tenantRepository.findById(tenantId).orElseThrow();
        assertEquals("acme-logistics", persistedTenant.getCode());
        assertEquals("Acme Logistics", persistedTenant.getName());

        HttpResponse<String> getResponse = get(tenantId);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"id\":\"" + tenantId + "\""));
        assertTrue(getResponse.body().contains("\"code\":\"acme-logistics\""));
        assertTrue(getResponse.body().contains("\"name\":\"Acme Logistics\""));
    }

    @Test
    void duplicateCodeReturnsConflictFromDatabaseUniqueConstraint() throws Exception {
        HttpResponse<String> firstResponse = postJson("""
                {"code":"duplicate-logistics","name":"First Tenant"}
                """);
        assertEquals(201, firstResponse.statusCode());

        HttpResponse<String> duplicateResponse = postJson("""
                {"code":"duplicate-logistics","name":"Second Tenant"}
                """);

        assertStandardError(duplicateResponse, 409, "TENANT_CODE_CONFLICT", "/api/v1/tenants");
        assertTrue(duplicateResponse.body().contains("\"message\":\"Tenant code already exists.\""));
    }

    @Test
    void unknownTenantReturnsNotFound() throws Exception {
        HttpResponse<String> response = get(UUID.randomUUID());

        assertStandardError(response, 404, "TENANT_NOT_FOUND", "/api/v1/tenants/" + extractPathId(response));
        assertTrue(response.body().contains("\"message\":\"Tenant not found.\""));
    }

    @Test
    void invalidCreateRequestRetainsValidationErrorContract() throws Exception {
        HttpResponse<String> response = postJson("""
                {"code":"INVALID_CODE","name":""}
                """);

        assertStandardError(response, 400, "VALIDATION_ERROR", "/api/v1/tenants");
        assertTrue(response.body().contains("\"field\":\"code\""));
        assertTrue(response.body().contains("\"field\":\"name\""));
    }

    private HttpResponse<String> postJson(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/tenants"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(UUID tenantId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/tenants/" + tenantId))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
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
        Instant.parse(extract(TIMESTAMP_PATTERN, response.body()));
    }

    private String extractPathId(HttpResponse<String> response) {
        return response.request().uri().getPath().substring("/api/v1/tenants/".length());
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "Expected field was not present in response: " + body);
        return matcher.group(1);
    }
}
