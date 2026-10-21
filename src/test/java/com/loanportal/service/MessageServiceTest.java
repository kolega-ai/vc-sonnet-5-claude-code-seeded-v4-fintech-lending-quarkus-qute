package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Message;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MessageServiceTest {

    @Inject
    MessageService messageService;

    @Test
    @TestTransaction
    void ownerCanPostAMessage() {
        User owner = TestFixtures.user("msg-a-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Msg Business A");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);

        Message message = messageService.post(app, owner, "Checking in on status");

        assertEquals("Checking in on status", message.body);
    }

    @Test
    @TestTransaction
    void anotherBorrowerCannotPostToThisApplication() {
        User owner = TestFixtures.user("msg-b-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User intruder = TestFixtures.user("msg-c-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Msg Business B");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);

        assertThrows(ForbiddenException.class, () -> messageService.post(app, intruder, "Hello"));
    }

    @Test
    @TestTransaction
    void emptyMessageIsRejected() {
        User owner = TestFixtures.user("msg-d-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Msg Business C");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);

        assertThrows(BadRequestException.class, () -> messageService.post(app, owner, "   "));
    }

    @Test
    @TestTransaction
    void staffCanPostToAnyApplication() {
        User owner = TestFixtures.user("msg-e-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User support = TestFixtures.user("msg-f-" + System.nanoTime() + "@test.local", Role.SUPPORT_SPECIALIST);
        BorrowerProfile profile = TestFixtures.profile(owner, "Msg Business D");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.IN_REVIEW);

        Message message = messageService.post(app, support, "We're reviewing your file");

        assertEquals(support.id, message.sender.id);
    }
}
