package com.loanportal.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentStatusTest {

    @Test
    void requestedMustBeUploadedBeforeReview() {
        assertTrue(DocumentStatus.REQUESTED.canTransitionTo(DocumentStatus.UPLOADED));
        assertFalse(DocumentStatus.REQUESTED.canTransitionTo(DocumentStatus.ACCEPTED));
    }

    @Test
    void onlyUploadedDocumentsCanBeAcceptedOrRejected() {
        assertTrue(DocumentStatus.UPLOADED.canTransitionTo(DocumentStatus.ACCEPTED));
        assertTrue(DocumentStatus.UPLOADED.canTransitionTo(DocumentStatus.REJECTED));
        assertFalse(DocumentStatus.ACCEPTED.canTransitionTo(DocumentStatus.REJECTED));
    }

    @Test
    void rejectedRequiresResubmissionBeforeReupload() {
        assertTrue(DocumentStatus.REJECTED.canTransitionTo(DocumentStatus.NEEDS_RESUBMISSION));
        assertFalse(DocumentStatus.REJECTED.canTransitionTo(DocumentStatus.ACCEPTED));
    }

    @Test
    void statesThatBlockADecisionAreCorrect() {
        assertTrue(DocumentStatus.REQUESTED.blocksDecision());
        assertTrue(DocumentStatus.UPLOADED.blocksDecision());
        assertTrue(DocumentStatus.REJECTED.blocksDecision());
        assertTrue(DocumentStatus.EXPIRED.blocksDecision());
        assertTrue(DocumentStatus.NEEDS_RESUBMISSION.blocksDecision());
        assertFalse(DocumentStatus.ACCEPTED.blocksDecision());
    }
}
