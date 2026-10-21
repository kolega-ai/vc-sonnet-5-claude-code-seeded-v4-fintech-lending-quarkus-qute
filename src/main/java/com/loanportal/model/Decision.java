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
@Table(name = "decision")
public class Decision extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    public LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false)
    public DecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false)
    public RiskTier riskTier;

    @Column(nullable = false, length = 2000)
    public String reasons;

    @Column(name = "counteroffer_amount")
    public BigDecimal counterofferAmount;

    @Column(name = "counteroffer_rate_pct")
    public BigDecimal counterofferRatePct;

    @Column(name = "counteroffer_term_months")
    public Integer counterofferTermMonths;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decided_by", nullable = false)
    public User decidedBy;

    @Column(name = "decided_at", nullable = false)
    public Instant decidedAt = Instant.now();
}
