package com.loanportal.service;

import com.loanportal.dto.BorrowerProfileForm;
import com.loanportal.dto.BusinessOwnerForm;
import com.loanportal.dto.PaymentAccountForm;
import com.loanportal.dto.RegisterForm;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.BusinessOwner;
import com.loanportal.model.PaymentAccount;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class BorrowerService {

    @Inject
    AuditService auditService;

    @Inject
    NotificationService notificationService;

    @Transactional
    public User register(RegisterForm form) {
        if (User.findByEmail(form.email) != null) {
            throw new jakarta.ws.rs.WebApplicationException("An account with that email already exists", 409);
        }
        User user = new User();
        user.email = form.email.trim().toLowerCase();
        user.password = BcryptUtil.bcryptHash(form.password);
        user.fullName = form.fullName;
        user.role = Role.BORROWER; // role is never accepted from the request
        user.active = true;
        user.persist();

        BorrowerProfile profile = new BorrowerProfile();
        profile.user = user;
        profile.businessName = form.businessName;
        profile.businessType = form.businessType;
        profile.taxId = form.taxId;
        profile.addressLine1 = form.addressLine1;
        profile.city = form.city;
        profile.state = form.state.toUpperCase();
        profile.postalCode = form.postalCode;
        profile.contactPhone = form.contactPhone;
        profile.persist();

        auditService.record(user, "BORROWER_REGISTERED", "User", user.id, null, null, "self-registration");
        notificationService.sendEmail(user.email, "Welcome to the Loan Origination Portal",
                "Your borrower account has been created. You can now start a loan application.");
        return user;
    }

    @Transactional
    public void updateProfile(BorrowerProfile profile, BorrowerProfileForm form, User actor) {
        String before = profile.businessName + "|" + profile.addressLine1 + "|" + profile.city;
        profile.businessName = form.businessName;
        profile.businessType = form.businessType;
        profile.addressLine1 = form.addressLine1;
        profile.addressLine2 = form.addressLine2;
        profile.city = form.city;
        profile.state = form.state.toUpperCase();
        profile.postalCode = form.postalCode;
        profile.contactPhone = form.contactPhone;
        profile.updatedAt = Instant.now();
        auditService.record(actor, "PROFILE_UPDATED", "BorrowerProfile", profile.id, before,
                form.businessName + "|" + form.addressLine1 + "|" + form.city, "self-service profile owner");
    }

    @Transactional
    public void updateSignInPhrase(User user, String newPhrase) {
        user.password = BcryptUtil.bcryptHash(newPhrase);
        user.persist();
        auditService.record(user, "CREDENTIAL_UPDATED", "User", user.id, null, null, "self-service, active session");
        notificationService.sendEmail(user.email, "Your sign-in phrase was changed",
                "The sign-in phrase for your Loan Origination Portal account was just changed.");
    }

    @Transactional
    public BusinessOwner addOwner(BorrowerProfile profile, BusinessOwnerForm form, User actor) {
        BusinessOwner owner = new BusinessOwner();
        owner.borrowerProfile = profile;
        owner.fullName = form.fullName;
        owner.title = form.title;
        owner.ownershipPct = form.ownershipPct;
        owner.ssnLast4 = form.ssnLast4;
        owner.dateOfBirth = form.dateOfBirth;
        owner.persist();
        auditService.record(actor, "BUSINESS_OWNER_ADDED", "BorrowerProfile", profile.id, null, form.fullName, "self-service profile owner");
        return owner;
    }

    @Transactional
    public PaymentAccount addPaymentAccount(BorrowerProfile profile, PaymentAccountForm form, User actor) {
        PaymentAccount account = new PaymentAccount();
        account.borrowerProfile = profile;
        account.bankName = form.bankName;
        account.accountType = form.accountType.toUpperCase();
        account.accountLast4 = form.accountLast4;
        account.routingLast4 = form.routingLast4;
        account.persist();
        auditService.record(actor, "PAYMENT_ACCOUNT_ADDED", "BorrowerProfile", profile.id, null,
                form.bankName + " ****" + form.accountLast4, "self-service profile owner");
        return account;
    }
}
