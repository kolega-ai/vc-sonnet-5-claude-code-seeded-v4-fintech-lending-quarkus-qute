package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_application")
public class LoanApplication extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrower_profile_id", nullable = false)
    public BorrowerProfile borrowerProfile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lending_product_id", nullable = false)
    public LendingProduct lendingProduct;

    @Column(name = "requested_amount", nullable = false)
    public BigDecimal requestedAmount;

    @Column(name = "requested_term_months", nullable = false)
    public Integer requestedTermMonths;

    @Column(nullable = false, length = 2000)
    public String purpose;

    @Column(name = "annual_revenue_cents")
    public Long annualRevenueCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ApplicationStatus status = ApplicationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false)
    public RiskTier riskTier = RiskTier.UNSCORED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_underwriter_id")
    public User assignedUnderwriter;

    @Column(name = "submitted_at")
    public Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public boolean isFinal() {
        return status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN;
    }
}
