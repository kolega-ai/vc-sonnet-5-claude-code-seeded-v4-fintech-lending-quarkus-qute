package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DocumentServiceTest {

    @Inject
    DocumentService documentService;

    @Test
    @TestTransaction
    void borrowerCannotUploadToAnotherBorrowersApplication() {
        User owner = TestFixtures.user("doc-a-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User intruder = TestFixtures.user("doc-b-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Doc Business A");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.SUBMITTED);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.REQUESTED);

        assertThrows(ForbiddenException.class, () ->
                documentService.upload(app, doc.id, intruder, new ByteArrayInputStream("x".getBytes()), "f.pdf", "application/pdf", 1));
    }

    @Test
    @TestTransaction
    void acceptOnlyAllowedFromUploaded() {
        User owner = TestFixtures.user("doc-c-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("doc-d-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Doc Business B");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.REQUESTED);

        assertThrows(BadRequestException.class, () -> documentService.accept(doc, underwriter));
    }

    @Test
    @TestTransaction
    void rejectRequiresABorrowerVisibleReason() {
        User owner = TestFixtures.user("doc-e-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("doc-f-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Doc Business C");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.UPLOADED);

        assertThrows(BadRequestException.class, () -> documentService.reject(doc, underwriter, ""));
    }

    @Test
    @TestTransaction
    void rejectedDocumentCanBeMovedToNeedsResubmission() {
        User owner = TestFixtures.user("doc-g-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("doc-h-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Doc Business D");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.REJECTED);

        documentService.requestResubmission(doc, underwriter, "Please resend a clearer scan");

        assertEquals(DocumentStatus.NEEDS_RESUBMISSION, doc.status);
    }

    @Test
    @TestTransaction
    void supportSpecialistCanReviewDocuments() {
        User owner = TestFixtures.user("doc-i-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User support = TestFixtures.user("doc-j-" + System.nanoTime() + "@test.local", Role.SUPPORT_SPECIALIST);
        BorrowerProfile profile = TestFixtures.profile(owner, "Doc Business E");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);
        DocumentTypeTemplate type = TestFixtures.requiredDocType();
        Document doc = TestFixtures.document(app, type, DocumentStatus.UPLOADED);

        documentService.accept(doc, support);

        assertEquals(DocumentStatus.ACCEPTED, doc.status);
    }
}
