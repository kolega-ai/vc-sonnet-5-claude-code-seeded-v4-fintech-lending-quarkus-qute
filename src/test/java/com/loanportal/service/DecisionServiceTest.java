package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loanportal.dto.DecisionForm;
import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.Document;
import com.loanportal.model.DocumentStatus;
import com.loanportal.model.DocumentTypeTemplate;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DecisionServiceTest {

    @Inject
    DecisionService decisionService;

    private DecisionForm approveForm() {
        DecisionForm form = new DecisionForm();
        form.decisionType = "APPROVE";
        form.riskTier = "LOW";
        form.reasons = "Looks good";
        return form;
    }

    @Test
    @TestTransaction
    void supportSpecialistCannotRecordADecision() {
        User owner = TestFixtures.user("owner-e-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User support = TestFixtures.user("support-" + System.nanoTime() + "@test.local", Role.SUPPORT_SPECIALIST);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business E");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);

        assertThrows(ForbiddenException.class, () -> decisionService.decide(app, support, approveForm()));
    }

    @Test
    @TestTransaction
    void approvalIsBlockedWhileARequiredDocumentIsMissing() {
        User owner = TestFixtures.user("owner-f-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("uw-f-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business F");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        TestFixtures.document(app, type, DocumentStatus.REQUESTED);

        assertThrows(BadRequestException.class, () -> decisionService.decide(app, underwriter, approveForm()));
    }

    @Test
    @TestTransaction
    void approvalIsBlockedWhileARequiredDocumentIsRejected() {
        User owner = TestFixtures.user("owner-g-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("uw-g-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business G");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.UPLOADED);
        doc.status = DocumentStatus.REJECTED;

        assertThrows(BadRequestException.class, () -> decisionService.decide(app, underwriter, approveForm()));
    }

    @Test
    @TestTransaction
    void approvalSucceedsOnceAllRequiredDocumentsAreAccepted() {
        User owner = TestFixtures.user("owner-h-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("uw-h-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business H");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        TestFixtures.document(app, type, DocumentStatus.ACCEPTED);

        decisionService.decide(app, underwriter, approveForm());

        org.junit.jupiter.api.Assertions.assertEquals(ApplicationStatus.APPROVED, app.status);
    }
}
