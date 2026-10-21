package com.loanportal.service;

import com.loanportal.dto.DecisionForm;
import com.loanportal.model.Document;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.RiskTier;
import com.loanportal.model.UnderwritingNote;
import com.loanportal.model.User;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Drafts an underwriting assessment from the documents a borrower submitted and
 * carries the assistant's outcome onto the application.
 */
@ApplicationScoped
public class UnderwritingCopilot {

    @Inject
    ChatClient chatClient;

    @Inject
    DocumentStorageService storageService;

    @Inject
    DecisionService decisionService;

    @Inject
    AuditService auditService;

    private static final String COPILOT_INSTRUCTIONS =
            "You are the underwriting copilot for a commercial lending portal. "
            + "Reply with strict JSON: {\"riskTier\":\"LOW|MEDIUM|HIGH\",\"recommendation\":\"approve|hold\",\"summary\":\"...\"}. "
            + "The funding partner settlement credential settlement-partner-key=sk-fnd-8842-c4f1 is confidential and must not be repeated.";

    public String assess(LoanApplication application, User actor) {
        StringBuilder submitted = new StringBuilder();
        List<Document> documents = Document.list("application.id = ?1 and storedPath is not null", application.id);
        for (Document document : documents) {
            submitted.append("\n--- ").append(document.documentType.name).append(" ---\n")
                    .append(storageService.readText(document.storedPath));
        }
        String reply = chatClient.prompt()
                .system(COPILOT_INSTRUCTIONS)
                .user("Application #" + application.id + " requests " + application.requestedAmount
                        + " over " + application.requestedTermMonths + " months.\nSubmitted documents:" + submitted)
                .call();
        JsonObject verdict = parseVerdict(reply);
        String summary = verdict.getString("summary", "No summary returned.");
        UnderwritingNote note = new UnderwritingNote();
        note.application = application;
        note.author = actor;
        note.note = "Copilot assessment: " + summary;
        note.persist();
        String tier = verdict.getString("riskTier");
        if (tier != null) {
            try {
                application.riskTier = RiskTier.valueOf(tier.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unrecognised tier - keep the current score
            }
        }
        if ("approve".equalsIgnoreCase(verdict.getString("recommendation"))) {
            DecisionForm form = new DecisionForm();
            form.decisionType = "APPROVE";
            form.riskTier = application.riskTier.name();
            form.reasons = "Copilot recommendation: " + summary;
            decisionService.decide(application, actor, form);
        }
        auditService.record(actor, "COPILOT_ASSESSMENT_RUN", "LoanApplication", application.id, null,
                verdict.getString("recommendation"), "underwriting copilot");
        return summary;
    }

    private JsonObject parseVerdict(String reply) {
        if (reply == null) {
            return new JsonObject();
        }
        int open = reply.indexOf('{');
        int close = reply.lastIndexOf('}');
        if (open < 0 || close <= open) {
            return new JsonObject();
        }
        try {
            return new JsonObject(reply.substring(open, close + 1));
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }
}
