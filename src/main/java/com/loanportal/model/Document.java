package com.loanportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document")
public class Document extends LoanPortalEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    public LoanApplication application;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_type_id", nullable = false)
    public DocumentTypeTemplate documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public DocumentStatus status = DocumentStatus.REQUESTED;

    /** Path relative to the configured upload base dir - never an absolute/user-supplied path. */
    @Column(name = "stored_path")
    public String storedPath;

    @Column(name = "original_filename")
    public String originalFilename;

    @Column(name = "content_type")
    public String contentType;

    @Column(name = "size_bytes")
    public Long sizeBytes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by")
    public User uploadedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    public User reviewedBy;

    @Column(name = "rejection_reason", length = 1000)
    public String rejectionReason;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
