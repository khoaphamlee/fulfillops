package com.fulfillops.inventory.application;

import com.fulfillops.inventory.domain.InventoryBalance;
import com.fulfillops.inventory.infrastructure.InventoryBalanceJdbcWriter;
import com.fulfillops.inventory.infrastructure.InventoryBalanceRepository;
import com.fulfillops.inventory.ledger.domain.InventoryLedgerEntry;
import com.fulfillops.inventory.ledger.infrastructure.InventoryLedgerEntryRepository;
import com.fulfillops.sku.application.SkuService;
import com.fulfillops.warehouse.application.WarehouseService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryBalanceJdbcWriter jdbcWriter;
    private final InventoryLedgerEntryRepository ledgerRepository;
    private final WarehouseService warehouseService;
    private final SkuService skuService;

    public InventoryService(InventoryBalanceRepository balanceRepository, InventoryBalanceJdbcWriter jdbcWriter, InventoryLedgerEntryRepository ledgerRepository, WarehouseService warehouseService, SkuService skuService) {
        this.balanceRepository = balanceRepository;
        this.jdbcWriter = jdbcWriter;
        this.ledgerRepository = ledgerRepository;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordReceiving(UUID tenantId, UUID warehouseId, List<ReceivingInventoryMovement> movements) {
        Instant mutationTime = Instant.now();
        ledgerRepository.saveAll(movements.stream()
                .map(movement -> InventoryLedgerEntry.receiving(tenantId, warehouseId, movement.skuId(), movement.receivingReceiptLineId(), movement.receivedQuantity(), mutationTime))
                .toList());
        ledgerRepository.flush();
        Map<UUID, Long> quantities = movements.stream().collect(Collectors.groupingBy(ReceivingInventoryMovement::skuId, Collectors.summingLong(ReceivingInventoryMovement::receivedQuantity)));
        quantities.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .map(entry -> new ReceivedSkuIncrement(entry.getKey(), entry.getValue()))
                .forEach(increment -> jdbcWriter.upsert(tenantId, warehouseId, increment, mutationTime));
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(UUID tenantId, UUID warehouseId, UUID skuId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        skuService.requireExistingSku(tenantId, skuId);
        return balanceRepository.findByTenantIdAndWarehouseIdAndSkuId(tenantId, warehouseId, skuId)
                .map(this::toResponse)
                .orElseGet(() -> new InventoryBalanceResponse(tenantId, warehouseId, skuId, 0, null, null));
    }

    private InventoryBalanceResponse toResponse(InventoryBalance balance) {
        return new InventoryBalanceResponse(balance.getTenantId(), balance.getWarehouseId(), balance.getSkuId(), balance.getOnHandQuantity(), balance.getCreatedAt(), balance.getUpdatedAt());
    }
}
