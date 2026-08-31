package com.fulfillops.warehouse.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.AbstractPostgresApplicationIntegrationTest;
import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
import com.fulfillops.warehouse.domain.Warehouse;
import com.fulfillops.warehouse.infrastructure.WarehouseRepository;
import com.fulfillops.warehouse.location.domain.WarehouseAisle;
import com.fulfillops.warehouse.location.domain.WarehouseBin;
import com.fulfillops.warehouse.location.domain.WarehouseRack;
import com.fulfillops.warehouse.location.domain.WarehouseZone;
import com.fulfillops.warehouse.location.infrastructure.WarehouseAisleRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseBinRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseRackRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseZoneRepository;
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

class WarehouseLocationApiIntegrationTests extends AbstractPostgresApplicationIntegrationTest {
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");
    @LocalServerPort private int port;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private WarehouseZoneRepository zoneRepository;
    @Autowired private WarehouseAisleRepository aisleRepository;
    @Autowired private WarehouseRackRepository rackRepository;
    @Autowired private WarehouseBinRepository binRepository;
    @Autowired private DataSource dataSource;

    @Test void hierarchyCanBeCreatedPersistedAndReadThroughApprovedRoutes() throws Exception {
        UUID tenantId = createTenant("location-hierarchy"); UUID warehouseId = createWarehouse(tenantId, "hcm-01");
        UUID zoneId = create(zonePath(tenantId, warehouseId), "ambient");
        UUID aisleId = create(aislePath(tenantId, warehouseId, zoneId), "a-01");
        UUID rackId = create(rackPath(tenantId, warehouseId, aisleId), "r-03");
        UUID binId = create(binPath(tenantId, warehouseId, rackId), "b-04");
        assertEquals(warehouseId, zoneRepository.findById(zoneId).orElseThrow().getWarehouseId());
        assertEquals(zoneId, aisleRepository.findById(aisleId).orElseThrow().getZoneId());
        assertEquals(aisleId, rackRepository.findById(rackId).orElseThrow().getAisleId());
        assertEquals(rackId, binRepository.findById(binId).orElseThrow().getRackId());
        assertEquals(200, get(zonePath(tenantId, warehouseId) + "/" + zoneId).statusCode());
        assertEquals(200, get(aislePath(tenantId, warehouseId, zoneId) + "/" + aisleId).statusCode());
        assertEquals(200, get(rackPath(tenantId, warehouseId, aisleId) + "/" + rackId).statusCode());
        assertEquals(200, get(binPath(tenantId, warehouseId, rackId) + "/" + binId).statusCode());
    }

    @Test void codesAreUniqueOnlyWithinTheirImmediateParentScope() throws Exception {
        UUID tenantId = createTenant("location-unique"); UUID firstWarehouse = createWarehouse(tenantId, "hcm-01"); UUID secondWarehouse = createWarehouse(tenantId, "han-01");
        UUID firstZone = create(zonePath(tenantId, firstWarehouse), "ambient");
        UUID secondZone = create(zonePath(tenantId, firstWarehouse), "chilled");
        assertEquals(201, post(zonePath(tenantId, secondWarehouse), "ambient").statusCode());
        assertError(post(zonePath(tenantId, firstWarehouse), "ambient"), 409, "ZONE_CODE_CONFLICT");
        UUID firstAisle = create(aislePath(tenantId, firstWarehouse, firstZone), "a-01");
        UUID secondAisle = create(aislePath(tenantId, firstWarehouse, secondZone), "a-01");
        assertError(post(aislePath(tenantId, firstWarehouse, firstZone), "a-01"), 409, "AISLE_CODE_CONFLICT");
        UUID firstRack = create(rackPath(tenantId, firstWarehouse, firstAisle), "r-01");
        UUID secondRack = create(rackPath(tenantId, firstWarehouse, secondAisle), "r-01");
        assertError(post(rackPath(tenantId, firstWarehouse, firstAisle), "r-01"), 409, "RACK_CODE_CONFLICT");
        create(binPath(tenantId, firstWarehouse, firstRack), "b-01");
        assertEquals(201, post(binPath(tenantId, firstWarehouse, secondRack), "b-01").statusCode());
        assertError(post(binPath(tenantId, firstWarehouse, firstRack), "b-01"), 409, "BIN_CODE_CONFLICT");
    }

    @Test void scopedPathsDoNotExposeCrossTenantWarehouseOrWrongParentResources() throws Exception {
        UUID tenantA = createTenant("location-scope-a"); UUID tenantB = createTenant("location-scope-b");
        UUID warehouseA = createWarehouse(tenantA, "hcm-01"); UUID warehouseAOther = createWarehouse(tenantA, "han-01"); UUID warehouseB = createWarehouse(tenantB, "hcm-01");
        UUID zoneA = create(zonePath(tenantA, warehouseA), "ambient"); UUID zoneB = create(zonePath(tenantB, warehouseB), "ambient");
        UUID aisleA = create(aislePath(tenantA, warehouseA, zoneA), "a-01"); UUID aisleB = create(aislePath(tenantB, warehouseB, zoneB), "a-01");
        UUID rackB = create(rackPath(tenantB, warehouseB, aisleB), "r-01"); UUID binB = create(binPath(tenantB, warehouseB, rackB), "b-01");
        assertError(get(zonePath(tenantA, warehouseA) + "/" + zoneB), 404, "ZONE_NOT_FOUND");
        assertError(get(zonePath(tenantA, warehouseAOther) + "/" + zoneA), 404, "ZONE_NOT_FOUND");
        assertError(get(aislePath(tenantA, warehouseA, zoneA) + "/" + aisleB), 404, "AISLE_NOT_FOUND");
        assertError(get(rackPath(tenantA, warehouseA, aisleA) + "/" + rackB), 404, "RACK_NOT_FOUND");
        assertError(get(binPath(tenantA, warehouseA, rackB) + "/" + binB), 404, "BIN_NOT_FOUND");
    }

    @Test void validationAndRequestIdUseExistingErrorContract() throws Exception {
        UUID tenantId = createTenant("location-validation"); UUID warehouseId = createWarehouse(tenantId, "hcm-01");
        HttpResponse<String> response = post(zonePath(tenantId, warehouseId), "INVALID_CODE");
        assertError(response, 400, "VALIDATION_ERROR"); assertTrue(response.body().contains("\"field\":\"code\""));
    }

    @Test void databaseForeignKeysAndParentScopedUniquenessRemainActive() throws Exception {
        UUID tenantId = createTenant("location-database"); UUID warehouseId = createWarehouse(tenantId, "hcm-01"); UUID zoneId = UUID.randomUUID();
        execute("INSERT INTO fulfillops.warehouse_zones (id, warehouse_id, code, created_at) VALUES (?, ?, 'ambient', CURRENT_TIMESTAMP)", zoneId, warehouseId);
        PSQLException duplicate = assertThrows(PSQLException.class, () -> execute("INSERT INTO fulfillops.warehouse_zones (id, warehouse_id, code, created_at) VALUES (?, ?, 'ambient', CURRENT_TIMESTAMP)", UUID.randomUUID(), warehouseId));
        assertEquals("uk_warehouse_zones_warehouse_code", duplicate.getServerErrorMessage().getConstraint());
        PSQLException foreignKey = assertThrows(PSQLException.class, () -> execute("INSERT INTO fulfillops.warehouse_aisles (id, zone_id, code, created_at) VALUES (?, ?, 'a-01', CURRENT_TIMESTAMP)", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals("fk_warehouse_aisles_zone", foreignKey.getServerErrorMessage().getConstraint());
    }

    @Test void openApiDocumentsAllLocationRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs"); assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones/{zoneId}/aisles"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/aisles/{aisleId}/racks"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/racks/{rackId}/bins"));
    }

    private UUID createTenant(String code) { return tenantRepository.saveAndFlush(Tenant.create(code, code)).getId(); }
    private UUID createWarehouse(UUID tenantId, String code) { return warehouseRepository.saveAndFlush(Warehouse.create(tenantId, code, code)).getId(); }
    private UUID create(String path, String code) throws Exception { HttpResponse<String> response = post(path, code); assertEquals(201, response.statusCode(), response.body()); UUID id = UUID.fromString(extract(ID_PATTERN, response.body())); assertEquals(path + "/" + id, response.headers().firstValue("Location").orElseThrow()); return id; }
    private HttpResponse<String> post(String path, String code) throws Exception { return request(path, "{\"code\":\"" + code + "\"}"); }
    private HttpResponse<String> request(String path, String body) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> get(String path) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private String zonePath(UUID tenantId, UUID warehouseId) { return "/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/zones"; }
    private String aislePath(UUID tenantId, UUID warehouseId, UUID zoneId) { return zonePath(tenantId, warehouseId) + "/" + zoneId + "/aisles"; }
    private String rackPath(UUID tenantId, UUID warehouseId, UUID aisleId) { return "/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/aisles/" + aisleId + "/racks"; }
    private String binPath(UUID tenantId, UUID warehouseId, UUID rackId) { return "/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/racks/" + rackId + "/bins"; }
    private void execute(String sql, Object... values) throws SQLException { try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]); statement.executeUpdate(); } }
    private void assertError(HttpResponse<String> response, int status, String code) { assertEquals(status, response.statusCode(), response.body()); assertTrue(response.body().contains("\"code\":\"" + code + "\"")); assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), extract(REQUEST_ID_PATTERN, response.body())); Instant.parse(extract(TIMESTAMP_PATTERN, response.body())); }
    private String extract(Pattern pattern, String body) { Matcher matcher = pattern.matcher(body); assertTrue(matcher.find(), body); return matcher.group(1); }
}
