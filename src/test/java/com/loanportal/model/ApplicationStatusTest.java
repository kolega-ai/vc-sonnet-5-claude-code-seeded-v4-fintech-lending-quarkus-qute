package com.loanportal.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationStatusTest {

    @Test
    void draftCanBeSubmittedOrWithdrawn() {
        assertTrue(ApplicationStatus.DRAFT.canTransitionTo(ApplicationStatus.SUBMITTED));
        assertTrue(ApplicationStatus.DRAFT.canTransitionTo(ApplicationStatus.WITHDRAWN));
        assertFalse(ApplicationStatus.DRAFT.canTransitionTo(ApplicationStatus.APPROVED));
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitions() {
        for (ApplicationStatus target : ApplicationStatus.values()) {
            assertFalse(ApplicationStatus.APPROVED.canTransitionTo(target));
            assertFalse(ApplicationStatus.REJECTED.canTransitionTo(target));
            assertFalse(ApplicationStatus.WITHDRAWN.canTransitionTo(target));
        }
    }

    @Test
    void inReviewCanReachAllDecisionOutcomes() {
        assertTrue(ApplicationStatus.IN_REVIEW.canTransitionTo(ApplicationStatus.APPROVED));
        assertTrue(ApplicationStatus.IN_REVIEW.canTransitionTo(ApplicationStatus.REJECTED));
        assertTrue(ApplicationStatus.IN_REVIEW.canTransitionTo(ApplicationStatus.COUNTEROFFERED));
        assertTrue(ApplicationStatus.IN_REVIEW.canTransitionTo(ApplicationStatus.NEEDS_DOCUMENTS));
    }

    @Test
    void cannotSkipDirectlyFromSubmittedToApproved() {
        assertFalse(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.APPROVED));
    }
}
