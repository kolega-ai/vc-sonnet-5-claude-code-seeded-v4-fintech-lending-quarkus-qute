package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Internal-only note. Must NEVER be rendered in a borrower-facing view or export. */
@Entity
@Table(name = "underwriting_note")
public class UnderwritingNote extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    public LoanApplication application;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    public User author;

    @Column(nullable = false, length = 4000)
    public String note;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
