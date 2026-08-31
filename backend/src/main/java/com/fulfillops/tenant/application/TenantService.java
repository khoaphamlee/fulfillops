package com.fulfillops.tenant.application;

import com.fulfillops.tenant.domain.Tenant;
import com.fulfillops.tenant.infrastructure.TenantRepository;
import com.fulfillops.tenant.presentation.CreateTenantRequest;
import com.fulfillops.tenant.presentation.TenantResponse;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private static final String TENANT_CODE_UNIQUE_CONSTRAINT = "uk_tenants_code";

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        Tenant tenant = Tenant.create(request.code(), request.name());

        try {
            return toResponse(tenantRepository.saveAndFlush(tenant));
        } catch (DataIntegrityViolationException exception) {
            if (isTenantCodeUniqueViolation(exception)) {
                throw new TenantCodeAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(UUID id) {
        Tenant tenant = findExistingTenant(id);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public void requireExistingTenant(UUID id) {
        findExistingTenant(id);
    }

    private Tenant findExistingTenant(UUID id) {
        return tenantRepository.findById(id).orElseThrow(TenantNotFoundException::new);
    }

    private boolean isTenantCodeUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getKind() == ConstraintKind.UNIQUE
                    && TENANT_CODE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }
}
