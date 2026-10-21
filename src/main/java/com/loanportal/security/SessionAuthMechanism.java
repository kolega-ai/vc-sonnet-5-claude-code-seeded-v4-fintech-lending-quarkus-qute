package com.loanportal.security;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Form-based session authentication: reads the signed session cookie, and if
 * valid, authenticates via a TrustedAuthenticationRequest (the password was
 * already checked once, at login time, by AuthService).
 */
@ApplicationScoped
public class SessionAuthMechanism implements HttpAuthenticationMechanism {

    public static final String CSRF_ATTRIBUTE = "lop.csrf";
    public static final String USER_ID_ATTRIBUTE = "lop.userId";

    @Inject
    SessionTokenService sessionTokenService;

    @ConfigProperty(name = "app.session.cookie-name")
    String cookieName;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        io.vertx.core.http.Cookie cookie = context.request().getCookie(cookieName);
        if (cookie == null) {
            return Uni.createFrom().nullItem();
        }
        SessionTokenService.ParsedSession parsed = sessionTokenService.verify(cookie.getValue());
        if (parsed == null) {
            return Uni.createFrom().nullItem();
        }
        context.put(CSRF_ATTRIBUTE, parsed.csrfToken());
        context.put(USER_ID_ATTRIBUTE, parsed.userId());
        AuthenticationRequest request = new TrustedAuthenticationRequest(String.valueOf(parsed.userId()));
        return identityProviderManager.authenticate(request);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        String redirectTo = context.request().uri();
        String location = "/login?redirect=" + java.net.URLEncoder.encode(redirectTo, java.nio.charset.StandardCharsets.UTF_8);
        return Uni.createFrom().item(new ChallengeData(302, "Location", location));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of(TrustedAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.COOKIE, cookieName));
    }
}
