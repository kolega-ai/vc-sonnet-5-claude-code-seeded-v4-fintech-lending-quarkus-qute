package com.loanportal.service;

import com.loanportal.model.LoanApplication;
import com.loanportal.model.NoticeTemplate;
import com.loanportal.model.OutboxEmail;
import com.loanportal.model.User;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Maintains and renders the editable borrower notice wording used on application events. */
@ApplicationScoped
public class NoticeTemplateService {

    @Inject AuditService auditService;

    private final Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);

    @Transactional
    public NoticeTemplate save(String code, String subject, String body, User actor) {
        NoticeTemplate template = NoticeTemplate.find("code = ?1", code).firstResult();
        boolean created = template == null;
        if (created) {
            template = new NoticeTemplate();
            template.code = code;
        }
        template.subject = subject;
        template.body = body;
        template.active = true;
        template.updatedAt = Instant.now();
        if (created) {
            template.persist();
        }
        auditService.record(actor, created ? "NOTICE_TEMPLATE_CREATED" : "NOTICE_TEMPLATE_UPDATED",
                "NoticeTemplate", template.id, null, code, "operations notice wording");
        return template;
    }

    /** Renders the stored wording for the supplied application and queues the produced notice. */
    @Transactional
    public OutboxEmail renderAndQueue(String code, LoanApplication app, User actor) throws Exception {
        NoticeTemplate template = NoticeTemplate.find("code = ?1 and active = true", code).firstResult();
        if (template == null) {
            throw new IllegalArgumentException("Unknown notice template: " + code);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("businessName", app.borrowerProfile.businessName);
        model.put("applicationId", app.id);
        model.put("amount", app.requestedAmount);
        model.put("status", app.status.name());
        StringWriter subjectOut = new StringWriter();
        new Template(code + "-subject", template.subject, cfg).process(model, subjectOut);
        StringWriter bodyOut = new StringWriter();
        new Template(code, template.body, cfg).process(model, bodyOut);
        OutboxEmail email = new OutboxEmail();
        email.toAddress = app.borrowerProfile.user.email;
        email.subject = subjectOut.toString();
        email.body = bodyOut.toString();
        email.status = "QUEUED";
        email.persist();
        auditService.record(actor, "NOTICE_TEMPLATE_RENDERED", "NoticeTemplate", template.id, null, code, "operations notice send");
        return email;
    }
}
