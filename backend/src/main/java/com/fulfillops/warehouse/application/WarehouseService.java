package com.fulfillops.warehouse.application;

import com.fulfillops.tenant.application.TenantService;
import com.fulfillops.warehouse.domain.Warehouse;
import com.fulfillops.warehouse.infrastructure.WarehouseRepository;
import com.fulfillops.warehouse.presentation.CreateWarehouseRequest;
import com.fulfillops.warehouse.presentation.WarehouseResponse;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private static final String WAREHOUSE_TENANT_CODE_UNIQUE_CONSTRAINT = "uk_warehouses_tenant_code";

    private final WarehouseRepository warehouseRepository;
    private final TenantService tenantService;

    public WarehouseService(WarehouseRepository warehouseRepository, TenantService tenantService) {
        this.warehouseRepository = warehouseRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public WarehouseResponse create(UUID tenantId, CreateWarehouseRequest request) {
        tenantService.requireExistingTenant(tenantId);
        Warehouse warehouse = Warehouse.create(tenantId, request.code(), request.name());

        try {
            return toResponse(warehouseRepository.saveAndFlush(warehouse));
        } catch (DataIntegrityViolationException exception) {
            if (isWarehouseTenantCodeUniqueViolation(exception)) {
                throw new WarehouseCodeAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID tenantId, UUID warehouseId) {
        tenantService.requireExistingTenant(tenantId);
        Warehouse warehouse = warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)
                .orElseThrow(WarehouseNotFoundException::new);
        return toResponse(warehouse);
    }

    private boolean isWarehouseTenantCodeUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getKind() == ConstraintKind.UNIQUE
                    && WAREHOUSE_TENANT_CODE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getTenantId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCreatedAt());
    }
}
