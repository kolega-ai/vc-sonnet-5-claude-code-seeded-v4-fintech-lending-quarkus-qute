package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "borrower_profile")
public class BorrowerProfile extends LoanPortalEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    public User user;

    @Column(name = "business_name", nullable = false)
    public String businessName;

    @Column(name = "business_type", nullable = false)
    public String businessType;

    /** Full tax ID (EIN). Only ever rendered masked in views/exports. */
    @Column(name = "tax_id", nullable = false)
    public String taxId;

    @Column(name = "years_in_business")
    public Integer yearsInBusiness;

    @Column(name = "annual_revenue_cents")
    public Long annualRevenueCents;

    @Column(name = "address_line1", nullable = false)
    public String addressLine1;

    @Column(name = "address_line2")
    public String addressLine2;

    @Column(nullable = false)
    public String city;

    @Column(nullable = false)
    public String state;

    @Column(name = "postal_code", nullable = false)
    public String postalCode;

    @Column(name = "contact_phone", nullable = false)
    public String contactPhone;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public static BorrowerProfile findByUserId(Long userId) {
        return find("user.id = ?1", userId).firstResult();
    }
}
