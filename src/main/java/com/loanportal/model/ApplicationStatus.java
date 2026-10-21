package com.loanportal.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Loan application state machine. Transitions are enforced server-side
 * exclusively via {@link #ALLOWED_TRANSITIONS} - see ApplicationWorkflowService.
 */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    NEEDS_DOCUMENTS,
    COUNTEROFFERED,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DRAFT, EnumSet.of(SUBMITTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(SUBMITTED, EnumSet.of(IN_REVIEW, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(IN_REVIEW, EnumSet.of(NEEDS_DOCUMENTS, COUNTEROFFERED, APPROVED, REJECTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(NEEDS_DOCUMENTS, EnumSet.of(IN_REVIEW, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(COUNTEROFFERED, EnumSet.of(IN_REVIEW, APPROVED, REJECTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(APPROVED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
