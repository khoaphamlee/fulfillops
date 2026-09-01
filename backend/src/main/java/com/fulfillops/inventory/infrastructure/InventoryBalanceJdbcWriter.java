package com.fulfillops.inventory.infrastructure;

import com.fulfillops.inventory.application.ReceivedSkuIncrement;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryBalanceJdbcWriter {
    private static final String UPSERT = """
            INSERT INTO fulfillops.inventory_balances (
                id, tenant_id, warehouse_id, sku_id, on_hand_quantity, created_at, updated_at
            ) VALUES (
                :id, :tenantId, :warehouseId, :skuId, :quantity, :mutationTime, :mutationTime
            )
            ON CONFLICT (tenant_id, warehouse_id, sku_id)
            DO UPDATE SET
                on_hand_quantity = fulfillops.inventory_balances.on_hand_quantity + EXCLUDED.on_hand_quantity,
                updated_at = EXCLUDED.updated_at
            """;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public InventoryBalanceJdbcWriter(NamedParameterJdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void upsert(UUID tenantId, UUID warehouseId, ReceivedSkuIncrement increment, Instant mutationTime) {
        jdbcTemplate.update(UPSERT, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("tenantId", tenantId)
                .addValue("warehouseId", warehouseId)
                .addValue("skuId", increment.skuId())
                .addValue("quantity", increment.receivedQuantity())
                .addValue("mutationTime", Timestamp.from(mutationTime)));
    }
}
