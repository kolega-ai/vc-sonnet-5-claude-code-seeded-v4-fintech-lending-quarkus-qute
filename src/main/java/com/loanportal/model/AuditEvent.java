package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only audit trail. No route exists to update or delete rows in this
 * table - only AuditService.record(...) may insert.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_user_id")
    public User actor;

    @Column(nullable = false)
    public String action;

    @Column(name = "entity_type", nullable = false)
    public String entityType;

    @Column(name = "entity_id")
    public Long entityId;

    @Column(name = "before_state", length = 4000)
    public String beforeState;

    @Column(name = "after_state", length = 4000)
    public String afterState;

    @Column(name = "auth_basis", nullable = false)
    public String authBasis;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
