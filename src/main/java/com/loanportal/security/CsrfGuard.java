package com.loanportal.security;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ForbiddenException;

/**
 * Synchronizer-token CSRF check for state-changing form posts. The expected
 * token is the per-session csrf value embedded in the signed session cookie
 * (see SessionTokenService/SessionAuthMechanism); every mutating form embeds
 * it as a hidden "csrf" field and every write endpoint calls {@link #verify}
 * before touching any data.
 */
@RequestScoped
@Named("csrf")
public class CsrfGuard {

    @Inject
    RoutingContext routingContext;

    public void verify(String submittedToken) {
        String expected = routingContext.get(SessionAuthMechanism.CSRF_ATTRIBUTE);
        if (expected == null || submittedToken == null || !expected.equals(submittedToken)) {
            throw new ForbiddenException("Invalid or missing CSRF token");
        }
    }

    public String currentToken() {
        return routingContext.get(SessionAuthMechanism.CSRF_ATTRIBUTE);
    }
}
