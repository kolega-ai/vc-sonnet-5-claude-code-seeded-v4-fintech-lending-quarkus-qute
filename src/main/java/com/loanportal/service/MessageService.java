package com.loanportal.service;

import com.loanportal.model.LoanApplication;
import com.loanportal.model.Message;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.util.List;

/** All Message rows are borrower-visible by construction; internal notes live in a separate entity/table. */
@ApplicationScoped
public class MessageService {

    @Inject
    AuditService auditService;

    @Inject
    NotificationService notificationService;

    @Transactional
    public Message post(LoanApplication app, User actor, String body) {
        if (actor.role == Role.BORROWER && !app.borrowerProfile.user.id.equals(actor.id)) {
            throw new ForbiddenException("Not your application");
        }
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }
        Message message = new Message();
        message.application = app;
        message.sender = actor;
        message.body = body.length() > 4000 ? body.substring(0, 4000) : body;
        message.persist();
        auditService.record(actor, "MESSAGE_POSTED", "LoanApplication", app.id, null, null, "conversation participant");

        User recipient = actor.role == Role.BORROWER ? staffRecipient(app) : app.borrowerProfile.user;
        if (recipient != null) {
            notificationService.notify(recipient, "New message on application #" + app.id, "/applications/" + app.id);
        }
        return message;
    }

    private User staffRecipient(LoanApplication app) {
        return app.assignedUnderwriter;
    }

    public List<Message> forApplication(Long applicationId) {
        return Message.list("application.id = ?1 order by createdAt asc", applicationId);
    }
}
