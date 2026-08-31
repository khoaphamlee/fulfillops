package com.fulfillops.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.AbstractPostgresApplicationIntegrationTest;
import com.fulfillops.inbound.application.InboundShipmentService;
import com.fulfillops.inbound.domain.InboundShipment;
import com.fulfillops.inbound.infrastructure.InboundShipmentLineRepository;
import com.fulfillops.inbound.infrastructure.InboundShipmentRepository;
import com.fulfillops.inbound.presentation.CreateInboundShipmentLineRequest;
import com.fulfillops.inbound.presentation.CreateInboundShipmentRequest;
import com.fulfillops.sku.domain.Sku;
import com.fulfillops.sku.infrastructure.SkuRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;

class InboundShipmentApiIntegrationTests extends AbstractPostgresApplicationIntegrationTest {
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    @LocalServerPort private int port;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SkuRepository skuRepository;
    @Autowired private InboundShipmentRepository shipmentRepository;
    @Autowired private InboundShipmentLineRepository lineRepository;
    @Autowired private InboundShipmentService shipmentService;
    @Autowired private DataSource dataSource;

    @Test void shipmentAndAllExpectedLinesAreCreatedAndReadAtomically() throws Exception {
        UUID tenantId = createTenant("inbound-happy"); UUID warehouseId = createWarehouse(tenantId, "hcm-01");
        UUID firstSku = createSku(tenantId, "ABC-001"); UUID secondSku = createSku(tenantId, "ABC-002");
        HttpResponse<String> response = createInbound(tenantId, warehouseId, firstSku, 24, secondSku, 12);
        assertEquals(201, response.statusCode(), response.body()); UUID shipmentId = UUID.fromString(extract(ID_PATTERN, response.body()));
        assertEquals(path(tenantId, warehouseId) + "/" + shipmentId, response.headers().firstValue("Location").orElseThrow());
        assertTrue(response.body().contains("\"tenantId\":\"" + tenantId + "\""));
        assertTrue(response.body().contains("\"warehouseId\":\"" + warehouseId + "\""));
        assertTrue(response.body().contains("\"expectedQuantity\":24")); assertTrue(response.body().contains("\"expectedQuantity\":12"));
        assertEquals(tenantId, shipmentRepository.findById(shipmentId).orElseThrow().getTenantId());
        assertEquals(warehouseId, shipmentRepository.findById(shipmentId).orElseThrow().getWarehouseId());
        assertEquals(2, lineRepository.findByInboundShipmentId(shipmentId).size());
        HttpResponse<String> getResponse = get(path(tenantId, warehouseId) + "/" + shipmentId);
        assertEquals(200, getResponse.statusCode()); assertTrue(getResponse.body().contains("\"expectedQuantity\":24"));
    }

    @Test void warehouseAndShipmentScopingDoNotExposeOtherResources() throws Exception {
        UUID tenantA = createTenant("inbound-scope-a"); UUID tenantB = createTenant("inbound-scope-b");
        UUID warehouseA = createWarehouse(tenantA, "hcm-01"); UUID warehouseAOther = createWarehouse(tenantA, "han-01"); UUID warehouseB = createWarehouse(tenantB, "hcm-01");
        UUID skuA = createSku(tenantA, "ABC-001"); UUID skuB = createSku(tenantB, "ABC-001");
        assertEquals(201, createInbound(tenantA, warehouseA, skuA, 1).statusCode());
        assertEquals(201, createInbound(tenantA, warehouseAOther, skuA, 1).statusCode());
        UUID shipmentB = UUID.fromString(extract(ID_PATTERN, createInbound(tenantB, warehouseB, skuB, 1).body()));
        assertError(get(path(tenantA, warehouseA) + "/" + shipmentB), 404, "INBOUND_SHIPMENT_NOT_FOUND");
        assertError(get(path(tenantB, warehouseAOther) + "/" + shipmentB), 404, "WAREHOUSE_NOT_FOUND");
        assertError(createInbound(tenantA, warehouseA, skuB, 1), 404, "SKU_NOT_FOUND");
    }

    @Test void quantitiesAndDuplicateSkusUseTheEstablishedContracts() throws Exception {
        UUID tenantId = createTenant("inbound-validation"); UUID warehouseId = createWarehouse(tenantId, "hcm-01"); UUID skuId = createSku(tenantId, "ABC-001");
        assertError(createInbound(tenantId, warehouseId, skuId, 0), 400, "VALIDATION_ERROR");
        assertError(createInbound(tenantId, warehouseId, skuId, -1), 400, "VALIDATION_ERROR");
        assertError(createInbound(tenantId, warehouseId, skuId, 1, skuId, 2), 409, "INBOUND_SHIPMENT_DUPLICATE_SKU_LINE");
    }

    @Test void databaseTenantAwareForeignKeysAndQuantityConstraintsRemainActive() throws Exception {
        UUID tenantA = createTenant("inbound-db-a"); UUID tenantB = createTenant("inbound-db-b");
        UUID warehouseA = createWarehouse(tenantA, "hcm-01"); UUID warehouseB = createWarehouse(tenantB, "hcm-01");
        UUID skuA = createSku(tenantA, "ABC-001"); UUID skuB = createSku(tenantB, "ABC-001"); UUID shipmentA = UUID.randomUUID();
        PSQLException warehouseMismatch = assertThrows(PSQLException.class, () -> insertShipment(UUID.randomUUID(), tenantA, warehouseB));
        assertEquals("fk_inbound_shipments_tenant_warehouse", warehouseMismatch.getServerErrorMessage().getConstraint());
        PSQLException unknownWarehouse = assertThrows(PSQLException.class, () -> insertShipment(UUID.randomUUID(), tenantA, UUID.randomUUID()));
        assertEquals("fk_inbound_shipments_tenant_warehouse", unknownWarehouse.getServerErrorMessage().getConstraint());
        insertShipment(shipmentA, tenantA, warehouseA);
        PSQLException skuMismatch = assertThrows(PSQLException.class, () -> insertLine(UUID.randomUUID(), shipmentA, tenantA, skuB, 1));
        assertEquals("fk_inbound_shipment_lines_tenant_sku", skuMismatch.getServerErrorMessage().getConstraint());
        PSQLException unknownSku = assertThrows(PSQLException.class, () -> insertLine(UUID.randomUUID(), shipmentA, tenantA, UUID.randomUUID(), 1));
        assertEquals("fk_inbound_shipment_lines_tenant_sku", unknownSku.getServerErrorMessage().getConstraint());
        PSQLException shipmentMismatch = assertThrows(PSQLException.class, () -> insertLine(UUID.randomUUID(), shipmentA, tenantB, skuB, 1));
        assertEquals("fk_inbound_shipment_lines_tenant_shipment", shipmentMismatch.getServerErrorMessage().getConstraint());
        insertLine(UUID.randomUUID(), shipmentA, tenantA, skuA, 1);
        PSQLException duplicate = assertThrows(PSQLException.class, () -> insertLine(UUID.randomUUID(), shipmentA, tenantA, skuA, 2));
        assertEquals("uk_inbound_shipment_lines_shipment_sku", duplicate.getServerErrorMessage().getConstraint());
        PSQLException quantity = assertThrows(PSQLException.class, () -> insertLine(UUID.randomUUID(), shipmentA, tenantA, createSku(tenantA, "ABC-002"), 0));
        assertEquals("chk_inbound_shipment_lines_expected_quantity", quantity.getServerErrorMessage().getConstraint());
    }

    @Test void failureAfterHeaderFlushRollsBackTheWholeAggregate() {
        UUID tenantId = createTenant("inbound-rollback"); UUID warehouseId = createWarehouse(tenantId, "hcm-01"); UUID skuId = createSku(tenantId, "ABC-001");
        assertThrows(DataIntegrityViolationException.class, () -> shipmentService.create(tenantId, warehouseId,
                new CreateInboundShipmentRequest(List.of(new CreateInboundShipmentLineRequest(skuId, 0L)))));
        assertFalse(shipmentRepository.findAll().iterator().hasNext()); assertFalse(lineRepository.findAll().iterator().hasNext());
    }

    @Test void openApiDocumentsInboundShipmentRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs"); assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}"));
    }

    private UUID createTenant(String code) { return tenantRepository.saveAndFlush(Tenant.create(code, code)).getId(); }
    private UUID createWarehouse(UUID tenantId, String code) { return warehouseRepository.saveAndFlush(Warehouse.create(tenantId, code, code)).getId(); }
    private UUID createSku(UUID tenantId, String code) { return skuRepository.saveAndFlush(Sku.create(tenantId, code, code)).getId(); }
    private HttpResponse<String> createInbound(UUID tenantId, UUID warehouseId, UUID firstSku, long firstQuantity) throws Exception { return createInbound(tenantId, warehouseId, firstSku, firstQuantity, null, null); }
    private HttpResponse<String> createInbound(UUID tenantId, UUID warehouseId, UUID firstSku, long firstQuantity, UUID secondSku, Integer secondQuantity) throws Exception {
        String secondLine = secondSku == null ? "" : ",{\"skuId\":\"" + secondSku + "\",\"expectedQuantity\":" + secondQuantity + "}";
        return post(path(tenantId, warehouseId), "{\"lines\":[{\"skuId\":\"" + firstSku + "\",\"expectedQuantity\":" + firstQuantity + "}" + secondLine + "]}");
    }
    private String path(UUID tenantId, UUID warehouseId) { return "/api/v1/tenants/" + tenantId + "/warehouses/" + warehouseId + "/inbound-shipments"; }
    private HttpResponse<String> post(String path, String body) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> get(String path) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private void insertShipment(UUID id, UUID tenantId, UUID warehouseId) throws SQLException { execute("INSERT INTO fulfillops.inbound_shipments (id, tenant_id, warehouse_id, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)", id, tenantId, warehouseId); }
    private void insertLine(UUID id, UUID shipmentId, UUID tenantId, UUID skuId, long quantity) throws SQLException { execute("INSERT INTO fulfillops.inbound_shipment_lines (id, inbound_shipment_id, tenant_id, sku_id, expected_quantity, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", id, shipmentId, tenantId, skuId, quantity); }
    private void execute(String sql, Object... values) throws SQLException { try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]); s.executeUpdate(); } }
    private void assertError(HttpResponse<String> response, int status, String code) { assertEquals(status, response.statusCode(), response.body()); assertTrue(response.body().contains("\"code\":\"" + code + "\"")); assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), extract(REQUEST_ID_PATTERN, response.body())); }
    private String extract(Pattern pattern, String body) { Matcher matcher = pattern.matcher(body); assertTrue(matcher.find(), body); return matcher.group(1); }
}
