package com.loanportal.service;

import com.loanportal.model.AuditEvent;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * The ONLY way to write to audit_event. No web route ever exposes update or
 * delete for this table - it is append-only by construction.
 */
@ApplicationScoped
public class AuditService {

    @Transactional
    public void record(User actor, String action, String entityType, Long entityId, String before, String after, String authBasis) {
        AuditEvent event = new AuditEvent();
        event.actor = actor;
        event.action = action;
        event.entityType = entityType;
        event.entityId = entityId;
        event.beforeState = truncate(before);
        event.afterState = truncate(after);
        event.authBasis = authBasis;
        event.persist();
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 3900 ? s.substring(0, 3900) : s;
    }
}
