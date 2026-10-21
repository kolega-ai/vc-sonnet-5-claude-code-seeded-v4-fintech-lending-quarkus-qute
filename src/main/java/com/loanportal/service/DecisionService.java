package com.loanportal.service;

import com.loanportal.dto.DecisionForm;
import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.Decision;
import com.loanportal.model.DecisionType;
import com.loanportal.model.Document;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.RiskTier;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class DecisionService {

    @Inject
    OwnershipService ownershipService;

    @Inject
    AuditService auditService;

    @Inject
    NotificationService notificationService;

    /**
     * Records a final (approve/reject) or counteroffer decision.
     * support_specialist is BLOCKED here server-side (not just hidden in the UI) -
     * ownershipService.assertCanDecide only allows UNDERWRITER/ADMIN.
     */
    @Transactional
    public Decision decide(LoanApplication app, User actor, DecisionForm form) {
        ownershipService.assertCanDecide(actor);

        DecisionType type;
        try {
            type = DecisionType.valueOf(form.decisionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown decision type");
        }
        RiskTier tier;
        try {
            tier = RiskTier.valueOf(form.riskTier.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown risk tier");
        }

        ApplicationStatus target = switch (type) {
            case APPROVE -> ApplicationStatus.APPROVED;
            case REJECT -> ApplicationStatus.REJECTED;
            case COUNTEROFFER -> ApplicationStatus.COUNTEROFFERED;
        };
        if (!app.status.canTransitionTo(target)) {
            throw new BadRequestException("Cannot record a " + type + " decision from status " + app.status);
        }

        if (type == DecisionType.APPROVE) {
            enforceDocumentGate(app);
        }
        if (type == DecisionType.COUNTEROFFER) {
            if (form.counterofferAmount == null || form.counterofferRatePct == null || form.counterofferTermMonths == null) {
                throw new BadRequestException("Counteroffer requires amount, rate, and term");
            }
        }

        Decision decision = new Decision();
        decision.application = app;
        decision.decisionType = type;
        decision.riskTier = tier;
        decision.reasons = form.reasons;
        decision.counterofferAmount = form.counterofferAmount;
        decision.counterofferRatePct = form.counterofferRatePct;
        decision.counterofferTermMonths = form.counterofferTermMonths;
        decision.decidedBy = actor;
        decision.persist();

        String before = app.status.name();
        app.status = target;
        app.riskTier = tier;
        app.updatedAt = Instant.now();

        auditService.record(actor, "DECISION_RECORDED", "LoanApplication", app.id, before, target.name() + " (" + type + ")",
                "underwriter/admin decision authority");
        notificationService.notify(app.borrowerProfile.user,
                "A decision has been recorded on your application #" + app.id + ": " + type.name(),
                "/applications/" + app.id);
        return decision;
    }

    /** A final approval cannot be recorded while required documents are missing, rejected, or expired. */
    private void enforceDocumentGate(LoanApplication app) {
        List<Document> docs = Document.list("application.id = ?1 and documentType.required = true", app.id);
        for (Document doc : docs) {
            if (doc.status.blocksDecision()) {
                throw new BadRequestException("Cannot approve: required document '" + doc.documentType.name
                        + "' is in state " + doc.status + " and must be ACCEPTED first");
            }
        }
    }
}
