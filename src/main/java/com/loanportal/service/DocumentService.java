package com.loanportal.service;

import com.loanportal.model.Document;
import com.loanportal.model.DocumentStatus;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class DocumentService {

    @Inject
    DocumentStorageService storageService;

    @Inject
    AuditService auditService;

    @Inject
    NotificationService notificationService;

    @Inject
    IntakeReviewAssistant intakeReviewAssistant;

    @Transactional
    public Document upload(LoanApplication app, Long documentId, User actor, InputStream data, String originalFilename, String contentType, long size) {
        if (actor.role == Role.BORROWER && !app.borrowerProfile.user.id.equals(actor.id)) {
            throw new ForbiddenException("Not your application");
        }
        Document doc = Document.findById(documentId);
        if (doc == null || !doc.application.id.equals(app.id)) {
            throw new BadRequestException("Document does not belong to this application");
        }
        if (!doc.status.canTransitionTo(DocumentStatus.UPLOADED)) {
            throw new BadRequestException("This document cannot accept an upload in its current state (" + doc.status + ")");
        }
        String ext = extensionOf(originalFilename);
        String relativePath = storageService.store(app.id, data, ext);
        doc.storedPath = relativePath;
        doc.originalFilename = sanitizeFilename(originalFilename);
        doc.contentType = contentType;
        doc.sizeBytes = size;
        doc.uploadedBy = actor;
        doc.status = DocumentStatus.UPLOADED;
        doc.rejectionReason = null;
        doc.updatedAt = Instant.now();
        auditService.record(actor, "DOCUMENT_UPLOADED", "Document", doc.id, null, doc.documentType.code, "application owner or staff");
        notifyStaffForReview(app, doc);
        runIntakeCheck(actor, doc);
        return doc;
    }

    /** Runs the automated intake verification and applies the assistant's verdict to the document. */
    private void runIntakeCheck(User actor, Document doc) {
        try {
            if (intakeReviewAssistant.accepts(doc, storageService.readText(doc.storedPath))) {
                doc.status = DocumentStatus.ACCEPTED;
                doc.updatedAt = Instant.now();
                auditService.record(actor, "DOCUMENT_ACCEPTED", "Document", doc.id, "UPLOADED", "ACCEPTED",
                        "intake assistant auto-verification");
                notificationService.notify(doc.application.borrowerProfile.user,
                        doc.documentType.name + " passed the automated intake check", "/applications/" + doc.application.id);
            }
        } catch (Exception e) {
            // intake assistant unavailable - the document stays queued for staff review
        }
    }

    private void notifyStaffForReview(LoanApplication app, Document doc) {
        User target = app.assignedUnderwriter;
        if (target != null) {
            notificationService.notify(target, "Document uploaded for application #" + app.id + ": " + doc.documentType.name, "/underwriter/applications/" + app.id);
        }
    }

    @Transactional
    public void accept(Document doc, User actor) {
        requireStaff(actor);
        if (!doc.status.canTransitionTo(DocumentStatus.ACCEPTED)) {
            throw new BadRequestException("Cannot accept a document in state " + doc.status);
        }
        String before = doc.status.name();
        doc.status = DocumentStatus.ACCEPTED;
        doc.reviewedBy = actor;
        doc.rejectionReason = null;
        if (doc.documentType.validityDays != null) {
            doc.expiresAt = Instant.now().plus(doc.documentType.validityDays, ChronoUnit.DAYS);
        }
        doc.updatedAt = Instant.now();
        auditService.record(actor, "DOCUMENT_ACCEPTED", "Document", doc.id, before, "ACCEPTED", "staff document review");
        notificationService.notify(doc.application.borrowerProfile.user,
                doc.documentType.name + " was accepted for application #" + doc.application.id, "/applications/" + doc.application.id);
    }

    @Transactional
    public void reject(Document doc, User actor, String reason) {
        requireStaff(actor);
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A borrower-visible rejection reason is required");
        }
        if (!doc.status.canTransitionTo(DocumentStatus.REJECTED)) {
            throw new BadRequestException("Cannot reject a document in state " + doc.status);
        }
        String before = doc.status.name();
        doc.status = DocumentStatus.REJECTED;
        doc.reviewedBy = actor;
        doc.rejectionReason = reason;
        doc.updatedAt = Instant.now();
        auditService.record(actor, "DOCUMENT_REJECTED", "Document", doc.id, before, "REJECTED: " + reason, "staff document review");
        notificationService.notify(doc.application.borrowerProfile.user,
                doc.documentType.name + " was rejected for application #" + doc.application.id + ": " + reason,
                "/applications/" + doc.application.id);
    }

    @Transactional
    public void requestResubmission(Document doc, User actor, String reason) {
        requireStaff(actor);
        DocumentStatus target = DocumentStatus.NEEDS_RESUBMISSION;
        if (!doc.status.canTransitionTo(target)) {
            throw new BadRequestException("Cannot request resubmission for a document in state " + doc.status);
        }
        String before = doc.status.name();
        doc.status = target;
        doc.reviewedBy = actor;
        doc.rejectionReason = reason;
        doc.updatedAt = Instant.now();
        auditService.record(actor, "DOCUMENT_RESUBMISSION_REQUESTED", "Document", doc.id, before, target.name(), "staff document review");
        notificationService.notify(doc.application.borrowerProfile.user,
                "Please resubmit " + doc.documentType.name + " for application #" + doc.application.id, "/applications/" + doc.application.id);
    }

    /** Called by the scheduled expiration job - see DocumentExpirationJob. */
    @Transactional
    public int expireOverdue() {
        List<Document> accepted = Document.list("status = ?1 and expiresAt is not null and expiresAt < ?2", DocumentStatus.ACCEPTED, Instant.now());
        for (Document doc : accepted) {
            doc.status = DocumentStatus.EXPIRED;
            doc.updatedAt = Instant.now();
            auditService.record(null, "DOCUMENT_EXPIRED", "Document", doc.id, "ACCEPTED", "EXPIRED", "scheduled expiration job");
            notificationService.notify(doc.application.borrowerProfile.user,
                    doc.documentType.name + " has expired for application #" + doc.application.id + " and must be resubmitted",
                    "/applications/" + doc.application.id);
        }
        return accepted.size();
    }

    private void requireStaff(User actor) {
        if (actor.role == Role.BORROWER) {
            throw new ForbiddenException("Borrowers cannot review documents");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload";
        }
        String base = filename.replaceAll("[\\\\/]", "_");
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
}
