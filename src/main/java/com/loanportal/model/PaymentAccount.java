package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Only masked account/routing fragments are ever stored - never full numbers. */
@Entity
@Table(name = "payment_account")
public class PaymentAccount extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrower_profile_id", nullable = false)
    public BorrowerProfile borrowerProfile;

    @Column(name = "bank_name", nullable = false)
    public String bankName;

    @Column(name = "account_type", nullable = false)
    public String accountType;

    @Column(name = "account_last4", nullable = false, length = 4)
    public String accountLast4;

    @Column(name = "routing_last4", nullable = false, length = 4)
    public String routingLast4;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
