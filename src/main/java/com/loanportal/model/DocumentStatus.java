package com.loanportal.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Document review state machine, enforced in DocumentService. */
public enum DocumentStatus {
    REQUESTED,
    UPLOADED,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    NEEDS_RESUBMISSION;

    public static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(DocumentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(REQUESTED, EnumSet.of(UPLOADED));
        ALLOWED_TRANSITIONS.put(UPLOADED, EnumSet.of(ACCEPTED, REJECTED));
        ALLOWED_TRANSITIONS.put(ACCEPTED, EnumSet.of(EXPIRED));
        ALLOWED_TRANSITIONS.put(REJECTED, EnumSet.of(NEEDS_RESUBMISSION));
        ALLOWED_TRANSITIONS.put(NEEDS_RESUBMISSION, EnumSet.of(UPLOADED));
        ALLOWED_TRANSITIONS.put(EXPIRED, EnumSet.of(NEEDS_RESUBMISSION));
    }

    public boolean canTransitionTo(DocumentStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /** Documents in these states block a final decision. */
    public boolean blocksDecision() {
        return this == REQUESTED || this == REJECTED || this == EXPIRED || this == NEEDS_RESUBMISSION || this == UPLOADED;
    }
}
