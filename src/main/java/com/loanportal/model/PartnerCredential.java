package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Derived verifier and salt for a partner's shared settlement phrase. */
@Entity
@Table(name = "partner_credential")
public class PartnerCredential extends LoanPortalEntity {

    @Column(name = "partner_code", nullable = false, unique = true)
    public String partnerCode;

    @Column(nullable = false)
    public String salt;

    @Column(nullable = false)
    public String verifier;

    @Column(nullable = false)
    public int iterations;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
