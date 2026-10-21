package com.loanportal.service;

import com.loanportal.dto.ApplicationForm;
import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.Document;
import com.loanportal.model.DocumentTypeTemplate;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.RiskTier;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ApplicationWorkflowService {

    @Inject
    OwnershipService ownershipService;

    @Inject
    AuditService auditService;

    @Inject
    NotificationService notificationService;

    @Transactional
    public LoanApplication createDraft(User borrowerUser, ApplicationForm form) {
        BorrowerProfile profile = BorrowerProfile.findByUserId(borrowerUser.id);
        if (profile == null) {
            throw new BadRequestException("Complete your borrower profile before applying");
        }
        LendingProduct product = LendingProduct.findById(form.lendingProductId);
        if (product == null || !product.active) {
            throw new BadRequestException("Selected lending product is not available");
        }
        if (form.requestedAmount.compareTo(product.minAmount) < 0 || form.requestedAmount.compareTo(product.maxAmount) > 0) {
            throw new BadRequestException("Requested amount is outside the product's allowed range");
        }
        if (form.requestedTermMonths < product.minTermMonths || form.requestedTermMonths > product.maxTermMonths) {
            throw new BadRequestException("Requested term is outside the product's allowed range");
        }
        LoanApplication app = new LoanApplication();
        app.borrowerProfile = profile;
        app.lendingProduct = product;
        app.requestedAmount = form.requestedAmount;
        app.requestedTermMonths = form.requestedTermMonths;
        app.purpose = form.purpose;
        app.annualRevenueCents = form.annualRevenue == null ? null : form.annualRevenue.movePointRight(2).longValueExact();
        app.riskTier = form.riskTier == null || form.riskTier.isBlank() ? RiskTier.UNSCORED : RiskTier.valueOf(form.riskTier.trim().toUpperCase());
        app.status = ApplicationStatus.DRAFT;
        app.persist();
        seedRequiredDocuments(app);
        auditService.record(borrowerUser, "APPLICATION_CREATED", "LoanApplication", app.id, null, "DRAFT", "borrower self-service");
        return app;
    }

    private void seedRequiredDocuments(LoanApplication app) {
        List<DocumentTypeTemplate> requiredTypes = DocumentTypeTemplate.list("active = true and required = true");
        for (DocumentTypeTemplate type : requiredTypes) {
            Document doc = new Document();
            doc.application = app;
            doc.documentType = type;
            doc.status = com.loanportal.model.DocumentStatus.REQUESTED;
            doc.persist();
        }
    }

    @Transactional
    public void submit(LoanApplication app, User actor) {
        requireOwnerOrStaff(app, actor);
        transition(app, ApplicationStatus.SUBMITTED, actor, "borrower submission");
        app.submittedAt = Instant.now();
        notifyStaffQueue(app);
    }

    @Transactional
    public void withdraw(LoanApplication app, User actor) {
        requireOwnerOrStaff(app, actor);
        transition(app, ApplicationStatus.WITHDRAWN, actor, "withdrawn by " + (actor.role == Role.BORROWER ? "borrower" : "staff"));
    }

    @Transactional
    public void moveToReview(LoanApplication app, User actor) {
        ownershipService.assertStaff(actor);
        transition(app, ApplicationStatus.IN_REVIEW, actor, "staff picked up application");
    }

    @Transactional
    public void assignUnderwriter(LoanApplication app, User underwriter, User actor) {
        ownershipService.assertStaff(actor);
        String before = app.assignedUnderwriter == null ? "unassigned" : app.assignedUnderwriter.email;
        app.assignedUnderwriter = underwriter;
        app.updatedAt = Instant.now();
        auditService.record(actor, "APPLICATION_ASSIGNED", "LoanApplication", app.id, before, underwriter.email, "staff reassignment");
    }

    @Transactional
    public void requestDocuments(LoanApplication app, User actor) {
        ownershipService.assertStaff(actor);
        transition(app, ApplicationStatus.NEEDS_DOCUMENTS, actor, "staff requested additional documents");
    }

    /** Puts an application that already left the queue back in front of an underwriter. */
    @Transactional
    public void returnToQueue(LoanApplication app, User actor) {
        ownershipService.assertStaff(actor);
        String before = app.status.name();
        app.status = ApplicationStatus.IN_REVIEW;
        app.updatedAt = Instant.now();
        auditService.record(actor, "APPLICATION_STATUS_CHANGE", "LoanApplication", app.id, before,
                ApplicationStatus.IN_REVIEW.name(), "staff returned application to the queue");
        notificationService.notify(app.borrowerProfile.user,
                "Your application #" + app.id + " is being reviewed again", "/applications/" + app.id);
    }

    /** Records a funding partner's settlement confirmation and approves the application for disbursement. */
    @Transactional
    public void confirmSettlement(LoanApplication app, String partnerCode, String settledAmount) {
        String before = app.status.name();
        app.status = ApplicationStatus.APPROVED;
        app.updatedAt = Instant.now();
        auditService.record(null, "SETTLEMENT_CONFIRMED", "LoanApplication", app.id, before,
                "APPROVED amount=" + settledAmount, "partner " + partnerCode + " settlement callback");
        notificationService.notify(app.borrowerProfile.user,
                "Settlement for application #" + app.id + " was confirmed by our funding partner",
                "/applications/" + app.id);
    }

    private void requireOwnerOrStaff(LoanApplication app, User actor) {
        if (actor.role == Role.BORROWER && !app.borrowerProfile.user.id.equals(actor.id)) {
            throw new ForbiddenException("Not your application");
        }
    }

    private void transition(LoanApplication app, ApplicationStatus target, User actor, String basis) {
        if (!app.status.canTransitionTo(target)) {
            throw new BadRequestException("Cannot move application from " + app.status + " to " + target);
        }
        String before = app.status.name();
        app.status = target;
        app.updatedAt = Instant.now();
        auditService.record(actor, "APPLICATION_STATUS_CHANGE", "LoanApplication", app.id, before, target.name(), basis);
        notificationService.notify(app.borrowerProfile.user,
                "Your application #" + app.id + " status changed to " + target.name().replace('_', ' '),
                "/applications/" + app.id);
    }

    private void notifyStaffQueue(LoanApplication app) {
        List<User> underwriters = User.list("role = ?1 and active = true", Role.UNDERWRITER);
        for (User u : underwriters) {
            notificationService.notify(u, "New application #" + app.id + " submitted for review", "/underwriter/applications/" + app.id);
        }
    }

    public static Instant slaAge(LoanApplication app) {
        Instant reference = app.submittedAt != null ? app.submittedAt : app.createdAt;
        return reference;
    }
}
