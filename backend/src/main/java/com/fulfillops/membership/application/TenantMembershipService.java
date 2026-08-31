package com.fulfillops.membership.application;

import com.fulfillops.membership.domain.TenantMembership;
import com.fulfillops.membership.domain.TenantRole;
import com.fulfillops.membership.infrastructure.TenantMembershipRepository;
import com.fulfillops.membership.presentation.CreateTenantMembershipRequest;
import com.fulfillops.membership.presentation.TenantMembershipResponse;
import com.fulfillops.tenant.application.TenantService;
import com.fulfillops.user.application.UserService;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantMembershipService {

    private static final String TENANT_MEMBERSHIP_UNIQUE_CONSTRAINT = "uk_tenant_memberships_tenant_user";

    private final TenantMembershipRepository tenantMembershipRepository;
    private final TenantService tenantService;
    private final UserService userService;

    public TenantMembershipService(
            TenantMembershipRepository tenantMembershipRepository,
            TenantService tenantService,
            UserService userService) {
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Transactional
    public TenantMembershipResponse create(UUID tenantId, CreateTenantMembershipRequest request) {
        tenantService.requireExistingTenant(tenantId);
        userService.requireExistingUser(request.userId());
        TenantMembership membership = TenantMembership.create(tenantId, request.userId());

        try {
            return toResponse(tenantMembershipRepository.saveAndFlush(membership));
        } catch (DataIntegrityViolationException exception) {
            if (isTenantMembershipUniqueViolation(exception)) {
                throw new TenantMembershipAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public TenantMembershipResponse getById(UUID tenantId, UUID membershipId) {
        tenantService.requireExistingTenant(tenantId);
        TenantMembership membership = findMembershipInTenant(tenantId, membershipId);
        return toResponse(membership);
    }

    @Transactional
    public TenantMembershipResponse changeRole(UUID tenantId, UUID membershipId, TenantRole role) {
        tenantService.requireExistingTenant(tenantId);
        TenantMembership membership = findMembershipInTenant(tenantId, membershipId);
        if (membership.changeRole(role)) {
            tenantMembershipRepository.flush();
        }
        return toResponse(membership);
    }

    private TenantMembership findMembershipInTenant(UUID tenantId, UUID membershipId) {
        return tenantMembershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(TenantMembershipNotFoundException::new);
    }

    private boolean isTenantMembershipUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getKind() == ConstraintKind.UNIQUE
                    && TENANT_MEMBERSHIP_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private TenantMembershipResponse toResponse(TenantMembership membership) {
        return new TenantMembershipResponse(
                membership.getId(),
                membership.getTenantId(),
                membership.getUserId(),
                membership.getRole(),
                membership.getCreatedAt(),
                membership.getUpdatedAt());
    }
}
