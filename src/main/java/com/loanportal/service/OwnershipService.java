package com.loanportal.service;

import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Centralised ownership/tenancy checks, deliberately kept one call frame above
 * the web controllers (ApplicationService/DocumentService call into this,
 * rather than controllers checking inline) so every access path is covered
 * uniformly regardless of which controller method calls in.
 * non-scoring: intentional architecture, not a vulnerability - see PRECISION_BASELINE.md item 1.
 */
@ApplicationScoped
public class OwnershipService {

    /** Staff (admin/underwriter/support) may view any application; a borrower only their own. */
    public LoanApplication loadApplicationForRead(Long applicationId, User actor) {
        LoanApplication app = LoanApplication.findById(applicationId);
        if (app == null) {
            throw new NotFoundException("Application not found");
        }
        if (actor.role == Role.BORROWER && !app.borrowerProfile.user.id.equals(actor.id)) {
            throw new ForbiddenException("You do not have access to this application");
        }
        return app;
    }

    public BorrowerProfile loadProfileForRead(Long profileId, User actor) {
        BorrowerProfile profile = BorrowerProfile.findById(profileId);
        if (profile == null) {
            throw new NotFoundException("Borrower profile not found");
        }
        if (actor.role == Role.BORROWER && !profile.user.id.equals(actor.id)) {
            throw new ForbiddenException("You do not have access to this profile");
        }
        return profile;
    }

    public void assertStaff(User actor) {
        if (actor.role == Role.BORROWER) {
            throw new ForbiddenException("Borrowers cannot access this resource");
        }
    }

    public void assertCanDecide(User actor) {
        if (actor.role != Role.UNDERWRITER && actor.role != Role.ADMIN) {
            throw new ForbiddenException("Only underwriters or admins may record a decision");
        }
    }

    public void assertAdmin(User actor) {
        if (actor.role != Role.ADMIN) {
            throw new ForbiddenException("Admin only");
        }
    }
}
