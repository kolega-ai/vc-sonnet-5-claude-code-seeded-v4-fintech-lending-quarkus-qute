package com.loanportal.security;

import com.loanportal.model.Notification;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Convenience accessor for the authenticated user backing the current SecurityIdentity.
 * Exposed to Qute templates as the "currentUser" CDI bean (e.g. {currentUser.user.fullName}).
 */
@RequestScoped
@Named("currentUser")
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    public boolean isAuthenticated() {
        return identity != null && !identity.isAnonymous();
    }

    public Long id() {
        if (!isAuthenticated()) {
            throw new NotAuthorizedException("Not authenticated");
        }
        return (Long) identity.getAttribute("userId");
    }

    public User user() {
        User u = inTransaction(() -> User.findById(id()));
        if (u == null || !u.active) {
            throw new NotAuthorizedException("Session user no longer active");
        }
        return u;
    }

    public Role role() {
        return user().role;
    }

    public long unreadNotificationCount() {
        if (!isAuthenticated()) {
            return 0;
        }
        return inTransaction(() -> Notification.count("user.id = ?1 and read = false", id()));
    }

    public List<Notification> recentNotifications() {
        if (!isAuthenticated()) {
            return List.of();
        }
        return inTransaction(() -> Notification.find("user.id = ?1 order by createdAt desc", id()).page(0, 6).list());
    }

    /**
     * Qute template rendering (e.g. the shared nav in layout/base.html) can run after
     * the JAX-RS resource method's own @Transactional scope has already closed, so
     * these lookups open a short-lived transaction of their own when none is active.
     */
    public static <T> T inTransaction(Supplier<T> work) {
        if (QuarkusTransaction.isActive()) {
            return work.get();
        }
        return QuarkusTransaction.requiringNew().call(work::get);
    }
}
