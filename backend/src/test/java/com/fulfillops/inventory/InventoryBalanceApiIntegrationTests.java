package com.fulfillops.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.inbound.domain.InboundShipment;
import com.fulfillops.inbound.domain.InboundShipmentLine;
import com.fulfillops.inbound.infrastructure.InboundShipmentLineRepository;
import com.fulfillops.inbound.infrastructure.InboundShipmentRepository;
import com.fulfillops.inventory.infrastructure.InventoryBalanceRepository;
import com.fulfillops.sku.domain.Sku;
import com.fulfillops.sku.infrastructure.SkuRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class InventoryBalanceApiIntegrationTests extends AbstractPostgresApplicationIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SkuRepository skuRepository;
    @Autowired private InboundShipmentRepository shipmentRepository;
    @Autowired private InboundShipmentLineRepository lineRepository;
    @Autowired private InventoryBalanceRepository balanceRepository;
    @Autowired private DataSource dataSource;

    @Test void getReturnsSyntheticZeroAndDoesNotCreateABalance() throws Exception {
        Fixture fixture = fixture("inventory-zero", "hcm-01", "ABC-001", 10);
        HttpResponse<String> response = get(balancePath(fixture));
        assertEquals(200, response.statusCode()); assertTrue(response.body().contains("\"onHandQuantity\":0")); assertTrue(response.body().contains("\"createdAt\":null")); assertTrue(response.body().contains("\"updatedAt\":null"));
        assertEquals(0, balanceRepository.count());
    }

    @Test void balanceReadIsScopedAndWarehouseSkuCellsAreIndependent() throws Exception {
        Fixture first = fixture("inventory-a", "hcm-01", "ABC-001", 10); assertEquals(201, receive(first, 4, "first-key").statusCode());
        UUID secondWarehouse = warehouseRepository.saveAndFlush(Warehouse.create(first.tenantId(), "han-01", "HAN")).getId();
        assertTrue(get("/api/v1/tenants/" + first.tenantId() + "/warehouses/" + secondWarehouse + "/inventory/skus/" + first.skuId()).body().contains("\"onHandQuantity\":0"));
        Fixture otherTenant = fixture("inventory-b", "hcm-01", "ABC-001", 10);
        assertEquals(404, get("/api/v1/tenants/" + otherTenant.tenantId() + "/warehouses/" + first.warehouseId() + "/inventory/skus/" + otherTenant.skuId()).statusCode());
        assertEquals(404, get("/api/v1/tenants/" + first.tenantId() + "/warehouses/" + first.warehouseId() + "/inventory/skus/" + otherTenant.skuId()).statusCode());
    }

    @Test void concurrentFirstBalanceCreationAcrossShipmentsDoesNotLoseQuantity() throws Exception {
        Fixture first = fixture("inventory-concurrent", "hcm-01", "ABC-001", 20);
        InboundShipment secondShipment = shipmentRepository.saveAndFlush(InboundShipment.create(first.tenantId(), first.warehouseId()));
        UUID secondLine = lineRepository.saveAndFlush(InboundShipmentLine.create(secondShipment.getId(), first.tenantId(), first.skuId(), 20)).getId();
        Fixture second = new Fixture(first.tenantId(), first.warehouseId(), first.skuId(), secondShipment.getId(), secondLine);
        CountDownLatch start = new CountDownLatch(1); ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<HttpResponse<String>> a = CompletableFuture.supplyAsync(() -> awaitReceive(start, first, 10, "shipment-a"), executor);
            CompletableFuture<HttpResponse<String>> b = CompletableFuture.supplyAsync(() -> awaitReceive(start, second, 15, "shipment-b"), executor);
            start.countDown(); assertEquals(201, a.get(30, TimeUnit.SECONDS).statusCode()); assertEquals(201, b.get(30, TimeUnit.SECONDS).statusCode());
            assertEquals(1, balanceRepository.countByTenantIdAndWarehouseIdAndSkuId(first.tenantId(), first.warehouseId(), first.skuId()));
            assertEquals(25, balanceRepository.findByTenantIdAndWarehouseIdAndSkuId(first.tenantId(), first.warehouseId(), first.skuId()).orElseThrow().getOnHandQuantity());
        } finally { executor.shutdownNow(); }
    }

    @Test void databaseConstraintsProtectBalanceCellAndTenantOwnership() throws Exception {
        Fixture first = fixture("inventory-db-a", "hcm-01", "ABC-001", 1); Fixture other = fixture("inventory-db-b", "hcm-01", "ABC-001", 1);
        insertBalance(UUID.randomUUID(), first.tenantId(), first.warehouseId(), first.skuId(), 0);
        PSQLException duplicate = assertThrows(PSQLException.class, () -> insertBalance(UUID.randomUUID(), first.tenantId(), first.warehouseId(), first.skuId(), 0));
        assertEquals("uk_inventory_balances_tenant_warehouse_sku", duplicate.getServerErrorMessage().getConstraint());
        PSQLException warehouseMismatch = assertThrows(PSQLException.class, () -> insertBalance(UUID.randomUUID(), first.tenantId(), other.warehouseId(), first.skuId(), 0));
        assertEquals("fk_inventory_balances_tenant_warehouse", warehouseMismatch.getServerErrorMessage().getConstraint());
        PSQLException skuMismatch = assertThrows(PSQLException.class, () -> insertBalance(UUID.randomUUID(), first.tenantId(), first.warehouseId(), other.skuId(), 0));
        assertEquals("fk_inventory_balances_tenant_sku", skuMismatch.getServerErrorMessage().getConstraint());
        UUID secondSku = skuRepository.saveAndFlush(Sku.create(first.tenantId(), "ABC-002", "Second SKU")).getId();
        PSQLException negative = assertThrows(PSQLException.class, () -> insertBalance(UUID.randomUUID(), first.tenantId(), first.warehouseId(), secondSku, -1));
        assertEquals("chk_inventory_balances_on_hand_quantity_non_negative", negative.getServerErrorMessage().getConstraint());
    }

    private Fixture fixture(String tenantCode, String warehouseCode, String skuCode, long expected) { UUID tenant = tenantRepository.saveAndFlush(Tenant.create(tenantCode, tenantCode)).getId(); UUID warehouse = warehouseRepository.saveAndFlush(Warehouse.create(tenant, warehouseCode, warehouseCode)).getId(); UUID sku = skuRepository.saveAndFlush(Sku.create(tenant, skuCode, skuCode)).getId(); InboundShipment shipment = shipmentRepository.saveAndFlush(InboundShipment.create(tenant, warehouse)); UUID line = lineRepository.saveAndFlush(InboundShipmentLine.create(shipment.getId(), tenant, sku, expected)).getId(); return new Fixture(tenant, warehouse, sku, shipment.getId(), line); }
    private String balancePath(Fixture fixture) { return "/api/v1/tenants/" + fixture.tenantId() + "/warehouses/" + fixture.warehouseId() + "/inventory/skus/" + fixture.skuId(); }
    private HttpResponse<String> awaitReceive(CountDownLatch latch, Fixture fixture, long quantity, String key) { try { latch.await(30, TimeUnit.SECONDS); return receive(fixture, quantity, key); } catch (Exception exception) { throw new RuntimeException(exception); } }
    private HttpResponse<String> receive(Fixture fixture, long quantity, String key) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/v1/tenants/" + fixture.tenantId() + "/warehouses/" + fixture.warehouseId() + "/inbound-shipments/" + fixture.shipmentId() + "/receipts")).header("Content-Type", "application/json").header("Idempotency-Key", key).POST(HttpRequest.BodyPublishers.ofString("{\"lines\":[{\"inboundShipmentLineId\":\"" + fixture.lineId() + "\",\"receivedQuantity\":" + quantity + "}]}" )).build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> get(String path) throws Exception { return HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private void insertBalance(UUID id, UUID tenant, UUID warehouse, UUID sku, long quantity) throws SQLException { try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO fulfillops.inventory_balances (id, tenant_id, warehouse_id, sku_id, on_hand_quantity, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) { statement.setObject(1, id); statement.setObject(2, tenant); statement.setObject(3, warehouse); statement.setObject(4, sku); statement.setLong(5, quantity); statement.executeUpdate(); } }
    private record Fixture(UUID tenantId, UUID warehouseId, UUID skuId, UUID shipmentId, UUID lineId) {}
}
