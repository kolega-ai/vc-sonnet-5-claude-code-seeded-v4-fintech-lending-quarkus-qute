package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Editable wording used when the portal notifies a borrower about an application event. */
@Entity
@Table(name = "notice_template")
public class NoticeTemplate extends LoanPortalEntity {

    @Column(nullable = false, unique = true)
    public String code;

    @Column(nullable = false)
    public String subject;

    @Column(nullable = false, length = 4000)
    public String body;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
