package com.fulfillops.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "fulfillops", name = "tenant_memberships")
public class TenantMembership {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantRole role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TenantMembership() {
    }

    private TenantMembership(UUID id, UUID tenantId, UUID userId) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = TenantRole.VIEWER;
    }

    public static TenantMembership create(UUID tenantId, UUID userId) {
        return new TenantMembership(UUID.randomUUID(), tenantId, userId);
    }

    @PrePersist
    void initializeCreatedAt() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public boolean changeRole(TenantRole role) {
        TenantRole requestedRole = Objects.requireNonNull(role, "role must not be null");
        if (this.role == requestedRole) {
            return false;
        }
        this.role = requestedRole;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public TenantRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
