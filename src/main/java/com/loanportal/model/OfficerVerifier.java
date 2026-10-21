package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Derived verifier for the counter phrase a branch officer quotes on the operations desk. */
@Entity
@Table(name = "officer_verifier")
public class OfficerVerifier extends LoanPortalEntity {

    @Column(name = "officer_code", nullable = false, unique = true)
    public String officerCode;

    @Column(nullable = false)
    public String verifier;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
