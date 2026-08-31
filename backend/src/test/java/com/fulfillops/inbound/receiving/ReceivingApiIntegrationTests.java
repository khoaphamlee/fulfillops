package com.fulfillops.inbound.receiving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.inbound.domain.InboundShipment;
import com.fulfillops.inbound.domain.InboundShipmentLine;
import com.fulfillops.inbound.infrastructure.InboundShipmentLineRepository;
import com.fulfillops.inbound.infrastructure.InboundShipmentRepository;
import com.fulfillops.inbound.receiving.application.ReceivingService;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptLineRepository;
import com.fulfillops.inbound.receiving.infrastructure.ReceivingReceiptRepository;
import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptLineRequest;
import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptRequest;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReceivingApiIntegrationTests {
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    @Container private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine").withDatabaseName("fulfillops_receiving_test").withUsername("fulfillops_test").withPassword("fulfillops_test");
    @LocalServerPort private int port;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SkuRepository skuRepository;
    @Autowired private InboundShipmentRepository shipmentRepository;
    @Autowired private InboundShipmentLineRepository plannedLineRepository;
    @Autowired private ReceivingReceiptRepository receiptRepository;
    @Autowired private ReceivingReceiptLineRepository receiptLineRepository;
    @Autowired private ReceivingService receivingService;
    @Autowired private DataSource dataSource;

    @DynamicPropertySource static void configureDatasource(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", POSTGRES::getJdbcUrl); registry.add("spring.datasource.username", POSTGRES::getUsername); registry.add("spring.datasource.password", POSTGRES::getPassword); }
    @AfterEach void clearData() { receiptLineRepository.deleteAll(); receiptRepository.deleteAll(); plannedLineRepository.deleteAll(); shipmentRepository.deleteAll(); skuRepository.deleteAll(); warehouseRepository.deleteAll(); tenantRepository.deleteAll(); }

    @Test void partialReceiptsAndDerivedProgressAreCorrect() throws Exception {
        Fixture fixture = fixture(100, 50);
        HttpResponse<String> first = receive(fixture, fixture.firstLineId(), 40); assertEquals(201, first.statusCode(), first.body());
        UUID receiptId = UUID.fromString(extract(ID_PATTERN, first.body())); assertEquals(receiptsPath(fixture) + "/" + receiptId, first.headers().firstValue("Location").orElseThrow());
        assertTrue(first.body().contains("\"cumulativeReceivedQuantity\":40"));
        assertEquals(201, receive(fixture, fixture.firstLineId(), 35).statusCode());
        HttpResponse<String> multiLine = receive(fixture, fixture.firstLineId(), 25, fixture.secondLineId(), 20); assertEquals(201, multiLine.statusCode());
        assertEquals(200, get(receiptsPath(fixture) + "/" + receiptId).statusCode());
        HttpResponse<String> progress = get(shipmentPath(fixture) + "/receiving-progress"); assertEquals(200, progress.statusCode());
        assertTrue(progress.body().contains("\"inboundShipmentLineId\":\"" + fixture.firstLineId() + "\""));
        assertTrue(progress.body().contains("\"expectedQuantity\":100")); assertTrue(progress.body().contains("\"receivedQuantity\":100")); assertTrue(progress.body().contains("\"remainingQuantity\":0"));
        assertTrue(progress.body().contains("\"expectedQuantity\":50")); assertTrue(progress.body().contains("\"receivedQuantity\":20")); assertTrue(progress.body().contains("\"remainingQuantity\":30"));
    }

    @Test void exactRemainingSucceedsAndOverReceiptAndDuplicateLinesFail() throws Exception {
        Fixture fixture = fixture(100, 10);
        assertEquals(201, receive(fixture, fixture.firstLineId(), 80).statusCode());
        assertEquals(201, receive(fixture, fixture.firstLineId(), 20).statusCode());
        assertError(receive(fixture, fixture.firstLineId(), 1), 409, "RECEIVING_QUANTITY_EXCEEDS_EXPECTED");
        assertError(receive(fixture, fixture.secondLineId(), 1, fixture.secondLineId(), 2), 409, "RECEIVING_DUPLICATE_LINE");
        assertError(receive(fixture, fixture.secondLineId(), 0), 400, "VALIDATION_ERROR");
        assertError(receive(fixture, fixture.secondLineId(), -1), 400, "VALIDATION_ERROR");
    }

    @Test void receiptScopingAndPlannedLineScopeDoNotExposeOtherResources() throws Exception {
        Fixture first = fixture(10, 10); UUID receiptId = UUID.fromString(extract(ID_PATTERN, receive(first, first.firstLineId(), 1).body()));
        UUID secondWarehouse = createWarehouse(first.tenantId(), "han-01"); InboundShipment otherShipment = shipmentRepository.saveAndFlush(InboundShipment.create(first.tenantId(), secondWarehouse));
        UUID otherLineId = plannedLineRepository.saveAndFlush(InboundShipmentLine.create(otherShipment.getId(), first.tenantId(), createSku(first.tenantId(), "ABC-003"), 1)).getId();
        assertError(get("/api/v1/tenants/" + first.tenantId() + "/warehouses/" + secondWarehouse + "/inbound-shipments/" + otherShipment.getId() + "/receipts/" + receiptId), 404, "RECEIVING_RECEIPT_NOT_FOUND");
        assertError(receive(first, otherLineId, 1), 404, "RECEIVING_PLANNED_LINE_NOT_FOUND");
    }

    @Test void databaseCompositeForeignKeysQuantityAndDuplicateConstraintsRemainActive() throws Exception {
        Fixture first = fixture(10, 10); UUID secondShipmentId = shipmentRepository.saveAndFlush(InboundShipment.create(first.tenantId(), first.warehouseId())).getId();
        UUID receiptId = UUID.randomUUID(); insertReceipt(receiptId, first.tenantId(), first.shipmentId());
        PSQLException receiptMismatch = assertThrows(PSQLException.class, () -> insertReceiptLine(UUID.randomUUID(), first.tenantId(), secondShipmentId, receiptId, first.firstLineId(), 1));
        assertEquals("fk_receiving_receipt_lines_tenant_shipment_receipt", receiptMismatch.getServerErrorMessage().getConstraint());
        UUID secondLineId = plannedLineRepository.saveAndFlush(InboundShipmentLine.create(secondShipmentId, first.tenantId(), createSku(first.tenantId(), "ABC-003"), 1)).getId();
        PSQLException plannedMismatch = assertThrows(PSQLException.class, () -> insertReceiptLine(UUID.randomUUID(), first.tenantId(), first.shipmentId(), receiptId, secondLineId, 1));
        assertEquals("fk_receiving_receipt_lines_tenant_shipment_planned_line", plannedMismatch.getServerErrorMessage().getConstraint());
        insertReceiptLine(UUID.randomUUID(), first.tenantId(), first.shipmentId(), receiptId, first.firstLineId(), 1);
        PSQLException duplicate = assertThrows(PSQLException.class, () -> insertReceiptLine(UUID.randomUUID(), first.tenantId(), first.shipmentId(), receiptId, first.firstLineId(), 1));
        assertEquals("uk_receiving_receipt_lines_receipt_planned_line", duplicate.getServerErrorMessage().getConstraint());
        PSQLException quantity = assertThrows(PSQLException.class, () -> insertReceiptLine(UUID.randomUUID(), first.tenantId(), first.shipmentId(), receiptId, first.secondLineId(), 0));
        assertEquals("chk_receiving_receipt_lines_received_quantity", quantity.getServerErrorMessage().getConstraint());
    }

    @Test void failureAfterReceiptRootFlushRollsBackReceiptAndLines() {
        Fixture fixture = fixture(10, 10);
        assertThrows(DataIntegrityViolationException.class, () -> receivingService.create(fixture.tenantId(), fixture.warehouseId(), fixture.shipmentId(), new CreateReceivingReceiptRequest(List.of(new CreateReceivingReceiptLineRequest(fixture.firstLineId(), 0L)))));
        assertFalse(receiptRepository.findAll().iterator().hasNext()); assertFalse(receiptLineRepository.findAll().iterator().hasNext());
    }

    @Test void pessimisticShipmentLockPreventsConcurrentOverReceipt() throws Exception {
        Fixture fixture = fixture(100, 1); assertEquals(201, receive(fixture, fixture.firstLineId(), 80).statusCode());
        CountDownLatch start = new CountDownLatch(1); ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<HttpResponse<String>> first = CompletableFuture.supplyAsync(() -> awaitReceive(start, fixture, 15), executor);
            CompletableFuture<HttpResponse<String>> second = CompletableFuture.supplyAsync(() -> awaitReceive(start, fixture, 15), executor);
            start.countDown(); HttpResponse<String> firstResponse = first.get(30, TimeUnit.SECONDS); HttpResponse<String> secondResponse = second.get(30, TimeUnit.SECONDS);
            long successes = List.of(firstResponse, secondResponse).stream().filter(response -> response.statusCode() == 201).count();
            long conflicts = List.of(firstResponse, secondResponse).stream().filter(response -> response.statusCode() == 409 && response.body().contains("RECEIVING_QUANTITY_EXCEEDS_EXPECTED")).count();
            assertEquals(1, successes); assertEquals(1, conflicts);
            long total = receiptLineRepository.findByTenantIdAndInboundShipmentId(fixture.tenantId(), fixture.shipmentId()).stream().filter(line -> line.getInboundShipmentLineId().equals(fixture.firstLineId())).mapToLong(line -> line.getReceivedQuantity()).sum();
            assertEquals(95, total);
        } finally { executor.shutdownNow(); }
    }

    @Test void openApiDocumentsReceivingRoutes() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs"); assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receipts"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receipts/{receiptId}"));
        assertTrue(response.body().contains("/api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receiving-progress"));
    }

    private HttpResponse<String> awaitReceive(CountDownLatch start, Fixture fixture, long quantity) { try { start.await(30, TimeUnit.SECONDS); return receive(fixture, fixture.firstLineId(), quantity); } catch (Exception exception) { throw new RuntimeException(exception); } }
    private Fixture fixture(long firstExpected, long secondExpected) { UUID tenantId = createTenant("receiving-" + UUID.randomUUID().toString().substring(0, 8)); UUID warehouseId = createWarehouse(tenantId, "hcm-01"); UUID firstSku = createSku(tenantId, "ABC-001"); UUID secondSku = createSku(tenantId, "ABC-002"); InboundShipment shipment = shipmentRepository.saveAndFlush(InboundShipment.create(tenantId, warehouseId)); InboundShipmentLine firstLine = plannedLineRepository.saveAndFlush(InboundShipmentLine.create(shipment.getId(), tenantId, firstSku, firstExpected)); InboundShipmentLine secondLine = plannedLineRepository.saveAndFlush(InboundShipmentLine.create(shipment.getId(), tenantId, secondSku, secondExpected)); return new Fixture(tenantId, warehouseId, shipment.getId(), firstLine.getId(), secondLine.getId()); }
    private UUID createTenant(String code) { return tenantRepository.saveAndFlush(Tenant.create(code, code)).getId(); }
    private UUID createWarehouse(UUID tenantId, String code) { return warehouseRepository.saveAndFlush(Warehouse.create(tenantId, code, code)).getId(); }
    private UUID createSku(UUID tenantId, String code) { return skuRepository.saveAndFlush(Sku.create(tenantId, code, code)).getId(); }
    private HttpResponse<String> receive(Fixture fixture, UUID firstLineId, long firstQuantity) throws Exception { return receive(fixture, firstLineId, firstQuantity, null, null); }
    private HttpResponse<String> receive(Fixture fixture, UUID firstLineId, long firstQuantity, UUID secondLineId, Integer secondQuantity) throws Exception { String second = secondLineId == null ? "" : ",{\"inboundShipmentLineId\":\"" + secondLineId + "\",\"receivedQuantity\":" + secondQuantity + "}"; return post(receiptsPath(fixture), "{\"lines\":[{\"inboundShipmentLineId\":\"" + firstLineId + "\",\"receivedQuantity\":" + firstQuantity + "}" + second + "]}"); }
    private String shipmentPath(Fixture fixture) { return "/api/v1/tenants/" + fixture.tenantId() + "/warehouses/" + fixture.warehouseId() + "/inbound-shipments/" + fixture.shipmentId(); }
    private String receiptsPath(Fixture fixture) { return shipmentPath(fixture) + "/receipts"; }
    private HttpResponse<String> post(String path, String body) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> get(String path) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private void insertReceipt(UUID id, UUID tenantId, UUID shipmentId) throws SQLException { execute("INSERT INTO fulfillops.receiving_receipts (id, tenant_id, inbound_shipment_id, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)", id, tenantId, shipmentId); }
    private void insertReceiptLine(UUID id, UUID tenantId, UUID shipmentId, UUID receiptId, UUID lineId, long quantity) throws SQLException { execute("INSERT INTO fulfillops.receiving_receipt_lines (id, tenant_id, inbound_shipment_id, receiving_receipt_id, inbound_shipment_line_id, received_quantity, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", id, tenantId, shipmentId, receiptId, lineId, quantity); }
    private void execute(String sql, Object... values) throws SQLException { try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) { for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]); statement.executeUpdate(); } }
    private void assertError(HttpResponse<String> response, int status, String code) { assertEquals(status, response.statusCode(), response.body()); assertTrue(response.body().contains("\"code\":\"" + code + "\"")); assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), extract(REQUEST_ID_PATTERN, response.body())); }
    private String extract(Pattern pattern, String body) { Matcher matcher = pattern.matcher(body); assertTrue(matcher.find(), body); return matcher.group(1); }
    private record Fixture(UUID tenantId, UUID warehouseId, UUID shipmentId, UUID firstLineId, UUID secondLineId) {}
}
