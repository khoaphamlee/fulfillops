package com.fulfillops.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.AbstractPostgresApplicationIntegrationTest;
import com.fulfillops.membership.domain.TenantMembership;
import com.fulfillops.membership.infrastructure.TenantMembershipRepository;
import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
import com.fulfillops.user.domain.User;
import com.fulfillops.user.infrastructure.UserRepository;
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

class UserAndTenantMembershipApiIntegrationTests extends AbstractPostgresApplicationIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");

    @LocalServerPort
    private int port;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository tenantMembershipRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void userCanBeCreatedPersistedCanonicalizedAndRead() throws Exception {
        HttpResponse<String> createResponse = postJson("/api/v1/users", """
                {"email":"Khoa@Example.COM","displayName":"Khoa Pham"}
                """);

        assertEquals(201, createResponse.statusCode());
        UUID userId = UUID.fromString(extract(ID_PATTERN, createResponse.body()));
        assertEquals("/api/v1/users/" + userId,
                createResponse.headers().firstValue("Location").orElseThrow());

        User persistedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("khoa@example.com", persistedUser.getEmail());
        assertEquals("Khoa Pham", persistedUser.getDisplayName());

        HttpResponse<String> getResponse = get("/api/v1/users/" + userId);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"email\":\"khoa@example.com\""));
    }

    @Test
    void caseVariantDuplicateEmailReturnsConflict() throws Exception {
        assertEquals(201, postJson("/api/v1/users", """
                {"email":"khoa@example.com","displayName":"First"}
                """).statusCode());

        HttpResponse<String> response = postJson("/api/v1/users", """
                {"email":"KHOA@EXAMPLE.COM","displayName":"Second"}
                """);

        assertStandardError(response, 409, "USER_EMAIL_CONFLICT", "/api/v1/users");
    }

    @Test
    void invalidUserRequestRetainsValidationErrorContract() throws Exception {
        HttpResponse<String> response = postJson("/api/v1/users", """
                {"email":"not-an-email","displayName":""}
                """);

        assertStandardError(response, 400, "VALIDATION_ERROR", "/api/v1/users");
        assertTrue(response.body().contains("\"field\":\"email\""));
        assertTrue(response.body().contains("\"field\":\"displayName\""));
    }

    @Test
    void databaseRejectsNoncanonicalUserEmail() {
        PSQLException exception = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.users (id, email, display_name, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), "Khoa@Example.com", "Khoa"));

        assertEquals("chk_users_email_canonical", exception.getServerErrorMessage().getConstraint());
    }

    @Test
    void membershipCanBeCreatedAndPersistsScalarReferences() throws Exception {
        UUID tenantId = createTenant("tenant-one");
        UUID userId = createUser("member@example.com");

        HttpResponse<String> response = postJson("/api/v1/tenants/" + tenantId + "/memberships", """
                {"userId":"%s"}
                """.formatted(userId));

        assertEquals(201, response.statusCode());
        UUID membershipId = UUID.fromString(extract(ID_PATTERN, response.body()));
        assertEquals("/api/v1/tenants/" + tenantId + "/memberships/" + membershipId,
                response.headers().firstValue("Location").orElseThrow());
        TenantMembership membership = tenantMembershipRepository.findById(membershipId).orElseThrow();
        assertEquals(tenantId, membership.getTenantId());
        assertEquals(userId, membership.getUserId());
        assertEquals("VIEWER", membership.getRole().name());

        HttpResponse<String> getResponse = get("/api/v1/tenants/" + tenantId + "/memberships/" + membershipId);
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"tenantId\":\"" + tenantId + "\""));
        assertTrue(getResponse.body().contains("\"userId\":\"" + userId + "\""));
        assertTrue(getResponse.body().contains("\"role\":\"VIEWER\""));
        assertTrue(getResponse.body().contains("\"updatedAt\":"));
    }

    @Test
    void userCanBelongToTwoTenantsAndTenantCanContainTwoUsers() throws Exception {
        UUID tenantOne = createTenant("tenant-one");
        UUID tenantTwo = createTenant("tenant-two");
        UUID userOne = createUser("user-one@example.com");
        UUID userTwo = createUser("user-two@example.com");

        assertEquals(201, createMembership(tenantOne, userOne).statusCode());
        assertEquals(201, createMembership(tenantTwo, userOne).statusCode());
        assertEquals(201, createMembership(tenantOne, userTwo).statusCode());
    }

    @Test
    void duplicateMembershipReturnsConflictFromDatabaseUniqueConstraint() throws Exception {
        UUID tenantId = createTenant("duplicate-tenant");
        UUID userId = createUser("duplicate@example.com");
        assertEquals(201, createMembership(tenantId, userId).statusCode());

        HttpResponse<String> response = createMembership(tenantId, userId);
        assertStandardError(response, 409, "TENANT_MEMBERSHIP_CONFLICT",
                "/api/v1/tenants/" + tenantId + "/memberships");
    }

    @Test
    void unknownUserAndTenantUseOwningFeatureErrors() throws Exception {
        UUID tenantId = createTenant("existing-tenant");
        HttpResponse<String> unknownUserResponse = createMembership(tenantId, UUID.randomUUID());
        assertStandardError(unknownUserResponse, 404, "USER_NOT_FOUND",
                "/api/v1/tenants/" + tenantId + "/memberships");

        HttpResponse<String> unknownTenantResponse = createMembership(UUID.randomUUID(), createUser("known@example.com"));
        assertStandardError(unknownTenantResponse, 404, "TENANT_NOT_FOUND",
                unknownTenantResponse.request().uri().getPath());
    }

    @Test
    void membershipGetIsTenantScoped() throws Exception {
        UUID tenantA = createTenant("tenant-a");
        UUID tenantB = createTenant("tenant-b");
        UUID userId = createUser("scoped@example.com");
        UUID membershipId = UUID.fromString(extract(ID_PATTERN, createMembership(tenantB, userId).body()));

        HttpResponse<String> response = get("/api/v1/tenants/" + tenantA + "/memberships/" + membershipId);
        assertStandardError(response, 404, "MEMBERSHIP_NOT_FOUND",
                "/api/v1/tenants/" + tenantA + "/memberships/" + membershipId);
    }

    @Test
    void roleChangePersistsAdvancesTimestampAndSameRoleIsANoOp() throws Exception {
        UUID tenantId = createTenant("role-change-tenant");
        UUID userId = createUser("role-change@example.com");
        UUID membershipId = UUID.fromString(extract(ID_PATTERN, createMembership(tenantId, userId).body()));
        TenantMembership beforeChange = tenantMembershipRepository.findById(membershipId).orElseThrow();
        Instant beforeUpdatedAt = beforeChange.getUpdatedAt();

        HttpResponse<String> changeResponse = patchJson(
                "/api/v1/tenants/" + tenantId + "/memberships/" + membershipId + "/role",
                """
                {"role":"ADMIN"}
                """);
        assertEquals(200, changeResponse.statusCode());
        assertTrue(changeResponse.body().contains("\"role\":\"ADMIN\""));

        TenantMembership afterChange = tenantMembershipRepository.findById(membershipId).orElseThrow();
        assertEquals("ADMIN", afterChange.getRole().name());
        assertTrue(afterChange.getUpdatedAt().isAfter(beforeUpdatedAt));
        Instant afterChangeUpdatedAt = afterChange.getUpdatedAt();

        HttpResponse<String> sameRoleResponse = patchJson(
                "/api/v1/tenants/" + tenantId + "/memberships/" + membershipId + "/role",
                """
                {"role":"ADMIN"}
                """);
        assertEquals(200, sameRoleResponse.statusCode());
        TenantMembership afterNoOp = tenantMembershipRepository.findById(membershipId).orElseThrow();
        assertEquals(afterChangeUpdatedAt, afterNoOp.getUpdatedAt());
    }

    @Test
    void roleChangeIsTenantScopedAndInvalidRoleRequestsUseExistingErrors() throws Exception {
        UUID tenantA = createTenant("role-tenant-a");
        UUID tenantB = createTenant("role-tenant-b");
        UUID membershipId = UUID.fromString(extract(ID_PATTERN,
                createMembership(tenantB, createUser("role-scope@example.com")).body()));

        HttpResponse<String> crossTenantResponse = patchJson(
                "/api/v1/tenants/" + tenantA + "/memberships/" + membershipId + "/role",
                """
                {"role":"ADMIN"}
                """);
        assertStandardError(crossTenantResponse, 404, "MEMBERSHIP_NOT_FOUND",
                "/api/v1/tenants/" + tenantA + "/memberships/" + membershipId + "/role");
        assertEquals("VIEWER", tenantMembershipRepository.findById(membershipId).orElseThrow().getRole().name());

        HttpResponse<String> invalidRoleResponse = patchJson(
                "/api/v1/tenants/" + tenantB + "/memberships/" + membershipId + "/role",
                """
                {"role":"OPERATOR"}
                """);
        assertStandardError(invalidRoleResponse, 400, "MALFORMED_JSON",
                "/api/v1/tenants/" + tenantB + "/memberships/" + membershipId + "/role");

        HttpResponse<String> missingRoleResponse = patchJson(
                "/api/v1/tenants/" + tenantB + "/memberships/" + membershipId + "/role",
                "{}"
        );
        assertStandardError(missingRoleResponse, 400, "VALIDATION_ERROR",
                "/api/v1/tenants/" + tenantB + "/memberships/" + membershipId + "/role");
    }

    @Test
    void databaseForeignKeysAndUniqueConstraintsRemainActive() throws Exception {
        UUID tenantId = createTenant("foreign-key-tenant");
        UUID userId = createUser("foreign-key@example.com");
        executeUpdate("""
                INSERT INTO fulfillops.tenant_memberships (id, tenant_id, user_id, role, created_at, updated_at)
                VALUES (?, ?, ?, 'VIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId, userId);

        PSQLException duplicate = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.tenant_memberships (id, tenant_id, user_id, role, created_at, updated_at)
                VALUES (?, ?, ?, 'VIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), tenantId, userId));
        assertEquals("uk_tenant_memberships_tenant_user", duplicate.getServerErrorMessage().getConstraint());

        PSQLException foreignKey = assertThrows(PSQLException.class, () -> executeUpdate("""
                INSERT INTO fulfillops.tenant_memberships (id, tenant_id, user_id, role, created_at, updated_at)
                VALUES (?, ?, ?, 'VIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), UUID.randomUUID(), userId));
        assertEquals("fk_tenant_memberships_tenant", foreignKey.getServerErrorMessage().getConstraint());
    }

    @Test
    void openApiDocumentsUserAndMembershipRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/users"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/memberships"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/memberships/{membershipId}/role"));
    }

    private UUID createTenant(String code) {
        return tenantRepository.saveAndFlush(Tenant.create(code, "Tenant " + code)).getId();
    }

    private UUID createUser(String email) {
        return userRepository.saveAndFlush(User.create(email, "User " + email)).getId();
    }

    private HttpResponse<String> createMembership(UUID tenantId, UUID userId) throws Exception {
        return postJson("/api/v1/tenants/" + tenantId + "/memberships", """
                {"userId":"%s"}
                """.formatted(userId));
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

    private HttpResponse<String> patchJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
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
