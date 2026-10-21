package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OwnershipServiceTest {

    @Inject
    OwnershipService ownershipService;

    @Test
    @TestTransaction
    void borrowerCannotReadAnotherBorrowersApplication() {
        User ownerA = TestFixtures.user("owner-a-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User ownerB = TestFixtures.user("owner-b-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profileA = TestFixtures.profile(ownerA, "Business A");
        LendingProduct product = TestFixtures.product();
        LoanApplication appA = TestFixtures.application(profileA, product, ApplicationStatus.SUBMITTED);

        assertThrows(ForbiddenException.class, () -> ownershipService.loadApplicationForRead(appA.id, ownerB));
    }

    @Test
    @TestTransaction
    void borrowerCanReadTheirOwnApplication() {
        User owner = TestFixtures.user("owner-c-" + System.nanoTime() + "@test.local", Role.BORROWER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business C");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.SUBMITTED);

        LoanApplication loaded = ownershipService.loadApplicationForRead(app.id, owner);
        assertEquals(app.id, loaded.id);
    }

    @Test
    @TestTransaction
    void staffCanReadAnyApplication() {
        User owner = TestFixtures.user("owner-d-" + System.nanoTime() + "@test.local", Role.BORROWER);
        User underwriter = TestFixtures.user("uw-" + System.nanoTime() + "@test.local", Role.UNDERWRITER);
        BorrowerProfile profile = TestFixtures.profile(owner, "Business D");
        LendingProduct product = TestFixtures.product();
        LoanApplication app = TestFixtures.application(profile, product, ApplicationStatus.SUBMITTED);

        LoanApplication loaded = ownershipService.loadApplicationForRead(app.id, underwriter);
        assertEquals(app.id, loaded.id);
    }
}
