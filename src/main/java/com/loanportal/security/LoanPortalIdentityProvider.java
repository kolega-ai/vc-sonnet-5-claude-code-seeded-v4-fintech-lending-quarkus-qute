package com.loanportal.security;

import com.loanportal.model.User;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds a {@link SecurityIdentity} for an already-verified session (the
 * session cookie signature/expiry was checked by SessionAuthMechanism before
 * this provider ever runs - this is NOT a password check).
 */
@ApplicationScoped
public class LoanPortalIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
        return context.runBlocking(() -> QuarkusTransaction.requiringNew().call(() -> {
            Long userId;
            try {
                userId = Long.parseLong(request.getPrincipal());
            } catch (NumberFormatException e) {
                return null;
            }
            User user = User.findById(userId);
            if (user == null || !user.active) {
                return null;
            }
            final String email = user.email;
            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(() -> email)
                    .addRole(user.role.name())
                    .addAttribute("userId", user.id)
                    .addAttribute("fullName", user.fullName)
                    .addCredential(new TokenCredential(String.valueOf(user.id), "session"))
                    .build();
        }));
    }
}
