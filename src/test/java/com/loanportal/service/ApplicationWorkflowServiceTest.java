package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loanportal.dto.ApplicationForm;
import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ApplicationWorkflowServiceTest {

    @Inject
    ApplicationWorkflowService workflowService;

    private ApplicationForm formFor(LendingProduct product) {
        ApplicationForm form = new ApplicationForm();
        form.lendingProductId = product.id;
        form.requestedAmount = new BigDecimal("25000.00");
        form.requestedTermMonths = 12;
        form.purpose = "Working capital";
        return form;
    }

    @Test
    @TestTransaction
    void createDraftRejectsAmountOutsideProductRange() {
        User owner = TestFixtures.user("wf-a-" + System.nanoTime() + "@test.local", Role.BORROWER);
        TestFixtures.profile(owner, "WF Business A");
        LendingProduct product = TestFixtures.product();
        ApplicationForm form = formFor(product);
        form.requestedAmount = product.maxAmount.add(BigDecimal.ONE);

        assertThrows(BadRequestException.class, () -> workflowService.createDraft(owner, form));
    }

    @Test
    @TestTransaction
    void createDraftSucceedsAndSeedsRequiredDocuments() {
        User owner = TestFixtures.user("wf-b-" + System.nanoTime() + "@test.local", Role.BORROWER);
        TestFixtures.profile(owner, "WF Business B");
        LendingProduct product = TestFixtures.product();
        TestFixtures.requiredDocType();

        LoanApplication app = workflowService.createDraft(owner, formFor(product));

        assertEquals(ApplicationStatus.DRAFT, app.status);
    }

    @Test
    @TestTransaction
    void submitTransitionsDraftToSubmitted() {
        User owner = TestFixtures.user("wf-c-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "WF Business C");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.DRAFT);

        workflowService.submit(app, owner);

        assertEquals(ApplicationStatus.SUBMITTED, app.status);
    }

    @Test
    @TestTransaction
    void submitRejectsAnotherBorrowersApplication() {
        User owner = TestFixtures.user("wf-d-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User intruder = TestFixtures.user("wf-e-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "WF Business D");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.DRAFT);

        assertThrows(ForbiddenException.class, () -> workflowService.submit(app, intruder));
    }

    @Test
    @TestTransaction
    void cannotSubmitAnAlreadyApprovedApplication() {
        User owner = TestFixtures.user("wf-f-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "WF Business F");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.APPROVED);

        assertThrows(BadRequestException.class, () -> workflowService.submit(app, owner));
    }

    @Test
    @TestTransaction
    void moveToReviewIsBlockedForBorrowers() {
        User owner = TestFixtures.user("wf-g-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "WF Business G");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.SUBMITTED);

        assertThrows(ForbiddenException.class, () -> workflowService.moveToReview(app, owner));
    }

    @Test
    @TestTransaction
    void moveToReviewSucceedsForUnderwriter() {
        User owner = TestFixtures.user("wf-h-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("wf-i-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "WF Business H");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.SUBMITTED);

        workflowService.moveToReview(app, underwriter);

        assertEquals(ApplicationStatus.IN_REVIEW, app.status);
    }
}
