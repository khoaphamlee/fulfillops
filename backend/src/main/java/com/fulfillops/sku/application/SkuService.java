package com.fulfillops.sku.application;

import com.fulfillops.sku.domain.Sku;
import com.fulfillops.sku.infrastructure.SkuRepository;
import com.fulfillops.sku.presentation.CreateSkuRequest;
import com.fulfillops.sku.presentation.SkuResponse;
import com.fulfillops.tenant.application.TenantService;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkuService {

    private static final String SKU_TENANT_CODE_UNIQUE_CONSTRAINT = "uk_skus_tenant_code";

    private final SkuRepository skuRepository;
    private final TenantService tenantService;

    public SkuService(SkuRepository skuRepository, TenantService tenantService) {
        this.skuRepository = skuRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public SkuResponse create(UUID tenantId, CreateSkuRequest request) {
        tenantService.requireExistingTenant(tenantId);
        Sku sku = Sku.create(tenantId, request.code(), request.name());
        try {
            return toResponse(skuRepository.saveAndFlush(sku));
        } catch (DataIntegrityViolationException exception) {
            if (isSkuTenantCodeUniqueViolation(exception)) {
                throw new SkuCodeAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public SkuResponse getById(UUID tenantId, UUID skuId) {
        tenantService.requireExistingTenant(tenantId);
        Sku sku = requireExistingSkuEntity(tenantId, skuId);
        return toResponse(sku);
    }

    @Transactional(readOnly = true)
    public void requireExistingSku(UUID tenantId, UUID skuId) {
        requireExistingSkuEntity(tenantId, skuId);
    }

    private Sku requireExistingSkuEntity(UUID tenantId, UUID skuId) {
        tenantService.requireExistingTenant(tenantId);
        return skuRepository.findByIdAndTenantId(skuId, tenantId)
                .orElseThrow(SkuNotFoundException::new);
    }

    private boolean isSkuTenantCodeUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getKind() == ConstraintKind.UNIQUE
                    && SKU_TENANT_CODE_UNIQUE_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private SkuResponse toResponse(Sku sku) {
        return new SkuResponse(sku.getId(), sku.getTenantId(), sku.getCode(), sku.getName(), sku.getCreatedAt());
    }
}
