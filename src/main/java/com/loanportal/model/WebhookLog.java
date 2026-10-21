package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Mock webhook delivery log, stands in for credit-bureau / core-banking callouts. */
@Entity
@Table(name = "webhook_log")
public class WebhookLog extends LoanPortalEntity {

    @Column(name = "event_type", nullable = false)
    public String eventType;

    @Column(nullable = false, length = 4000)
    public String payload;

    @Column(nullable = false)
    public String status;

    @Column(name = "attempted_at", nullable = false)
    public Instant attemptedAt = Instant.now();
}
