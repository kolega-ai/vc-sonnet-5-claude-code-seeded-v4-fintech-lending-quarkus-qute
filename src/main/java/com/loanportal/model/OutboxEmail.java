package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Mock outbound email channel: rows here stand in for a real ESP/SMTP integration. */
@Entity
@Table(name = "outbox_email")
public class OutboxEmail extends LoanPortalEntity {

    @Column(name = "to_address", nullable = false)
    public String toAddress;

    @Column(nullable = false)
    public String subject;

    @Column(nullable = false, length = 8000)
    public String body;

    @Column(nullable = false)
    public String status = "SENT";

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
