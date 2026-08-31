package com.fulfillops.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantMembership() {
    }

    private TenantMembership(UUID id, UUID tenantId, UUID userId) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
    }

    public static TenantMembership create(UUID tenantId, UUID userId) {
        return new TenantMembership(UUID.randomUUID(), tenantId, userId);
    }

    @PrePersist
    void initializeCreatedAt() {
        createdAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
