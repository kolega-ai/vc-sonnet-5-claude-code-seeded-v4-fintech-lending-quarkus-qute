package com.loanportal.service;

import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.Document;
import com.loanportal.model.DocumentStatus;
import com.loanportal.model.DocumentTypeTemplate;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.RiskTier;
import com.loanportal.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.math.BigDecimal;

/** Shared fixture builders for integration tests - creates minimal valid rows, no mocking of Panache. */
public final class TestFixtures {

    private TestFixtures() {}

    public static User user(String email, Role role) {
        User u = new User();
        u.email = email;
        u.password = BcryptUtil.bcryptHash("Test!Password123");
        u.fullName = "Test " + role;
        u.role = role;
        u.active = true;
        u.persist();
        return u;
    }

    public static BorrowerProfile profile(User owner, String businessName) {
        BorrowerProfile p = new BorrowerProfile();
        p.user = owner;
        p.businessName = businessName;
        p.businessType = "LLC";
        p.taxId = "84-9999999";
        p.addressLine1 = "1 Test St";
        p.city = "Testville";
        p.state = "CA";
        p.postalCode = "90001";
        p.contactPhone = "555-000-0000";
        p.persist();
        return p;
    }

    public static LendingProduct product() {
        LendingProduct p = new LendingProduct();
        p.name = "Test Product " + System.nanoTime();
        p.description = "Test";
        p.minAmount = new BigDecimal("1000.00");
        p.maxAmount = new BigDecimal("500000.00");
        p.minTermMonths = 1;
        p.maxTermMonths = 60;
        p.baseRatePct = new BigDecimal("8.00");
        p.active = true;
        p.persist();
        return p;
    }

    public static LoanApplication application(BorrowerProfile profile, LendingProduct product, ApplicationStatus status) {
        LoanApplication app = new LoanApplication();
        app.borrowerProfile = profile;
        app.lendingProduct = product;
        app.requestedAmount = new BigDecimal("25000.00");
        app.requestedTermMonths = 12;
        app.purpose = "Testing";
        app.status = status;
        app.riskTier = RiskTier.UNSCORED;
        app.persist();
        return app;
    }

    public static DocumentTypeTemplate requiredDocType() {
        DocumentTypeTemplate t = new DocumentTypeTemplate();
        t.code = "test_doc_" + System.nanoTime();
        t.name = "Test Document";
        t.description = "Test";
        t.required = true;
        t.active = true;
        t.persist();
        return t;
    }

    public static Document document(LoanApplication app, DocumentTypeTemplate type, DocumentStatus status) {
        Document d = new Document();
        d.application = app;
        d.documentType = type;
        d.status = status;
        d.persist();
        return d;
    }
}
