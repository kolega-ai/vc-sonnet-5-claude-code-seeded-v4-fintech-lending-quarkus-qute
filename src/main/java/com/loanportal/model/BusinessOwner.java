package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "business_owner")
public class BusinessOwner extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrower_profile_id", nullable = false)
    public BorrowerProfile borrowerProfile;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Column(nullable = false)
    public String title;

    @Column(name = "ownership_pct", nullable = false)
    public BigDecimal ownershipPct;

    /** Only the last 4 digits are ever stored. */
    @Column(name = "ssn_last4", nullable = false, length = 4)
    public String ssnLast4;

    @Column(name = "date_of_birth")
    public String dateOfBirth;

    public static java.util.List<BusinessOwner> findByProfile(Long profileId) {
        return list("borrowerProfile.id = ?1", profileId);
    }
}
