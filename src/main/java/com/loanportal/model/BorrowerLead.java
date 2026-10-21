package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Row produced by an admin CSV lead import. Not a live borrower account until manually converted. */
@Entity
@Table(name = "borrower_lead")
public class BorrowerLead extends LoanPortalEntity {

    @Column(name = "business_name", nullable = false)
    public String businessName;

    @Column(name = "contact_email", nullable = false)
    public String contactEmail;

    @Column(name = "contact_phone")
    public String contactPhone;

    @Column(nullable = false)
    public String status = "NEW";

    @Column(name = "import_batch", nullable = false)
    public String importBatch;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
