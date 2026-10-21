package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "lending_product")
public class LendingProduct extends LoanPortalEntity {

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public String description;

    @Column(name = "min_amount", nullable = false)
    public BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false)
    public BigDecimal maxAmount;

    @Column(name = "min_term_months", nullable = false)
    public Integer minTermMonths;

    @Column(name = "max_term_months", nullable = false)
    public Integer maxTermMonths;

    @Column(name = "base_rate_pct", nullable = false)
    public BigDecimal baseRatePct;

    @Column(nullable = false)
    public boolean active = true;
}
