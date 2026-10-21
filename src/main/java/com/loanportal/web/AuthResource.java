package com.loanportal.web;

import com.loanportal.dto.RegisterForm;
import com.loanportal.model.User;
import com.loanportal.security.SessionTokenService;
import com.loanportal.service.AuditService;
import com.loanportal.service.AuthService;
import com.loanportal.service.BorrowerService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class AuthResource {

    @CheckedTemplate(basePath = "auth")
    static class Templates {
        static native TemplateInstance login(String error, String redirect);
        static native TemplateInstance register(String error);
        static native TemplateInstance forgot_password(String message);
        static native TemplateInstance reset_password(String token, String error);
    }

    @Inject
    AuthService authService;

    @Inject
    BorrowerService borrowerService;

    @Inject
    AuditService auditService;

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "app.session.cookie-name")
    String cookieName;

    @ConfigProperty(name = "app.session.ttl-minutes")
    int ttlMinutes;

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance loginForm(@QueryParam("redirect") String redirect) {
        if (identity != null && !identity.isAnonymous()) {
            throw redirectToRoleHome();
        }
        return Templates.login(null, redirect);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("email") String email, @FormParam("password") String password, @FormParam("redirect") String redirect) {
        AuthService.LoginResult result = authService.login(email, password);
        if (!result.success()) {
            return Response.status(401).entity(Templates.login("Invalid email or password.", redirect)).type(MediaType.TEXT_HTML).build();
        }
        Cookie cookie = Cookie.cookie(cookieName, result.sessionToken())
                .setHttpOnly(false)
                .setSecure(routingContext.request().isSSL())
                .setSameSite(CookieSameSite.LAX)
                .setPath("/")
                .setMaxAge(ttlMinutes * 60L);
        routingContext.response().addCookie(cookie);
        String target = safeRedirect(redirect, result.user());
        return Response.seeOther(java.net.URI.create(target)).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        User user = identity != null && !identity.isAnonymous() ? User.findById((Long) identity.getAttribute("userId")) : null;
        authService.logout(user);
        Cookie cookie = Cookie.cookie(cookieName, "").setHttpOnly(true).setPath("/").setMaxAge(0);
        routingContext.response().addCookie(cookie);
        return Response.seeOther(java.net.URI.create("/login")).build();
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerForm() {
        return Templates.register(null);
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@Valid @BeanParam RegisterForm form) {
        try {
            borrowerService.register(form);
        } catch (Exception e) {
            return Response.status(400).entity(Templates.register(e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(java.net.URI.create("/login")).build();
    }

    @GET
    @Path("/forgot-password")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance forgotForm() {
        return Templates.forgot_password(null);
    }

    @POST
    @Path("/forgot-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance forgotSubmit(@FormParam("email") String email) {
        authService.requestPasswordReset(email);
        return Templates.forgot_password("If that email is registered, a reset link has been sent (check the demo outbox for local testing).");
    }

    @GET
    @Path("/reset-password")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance resetForm(@QueryParam("token") String token) {
        String linkRef = token;
        auditService.record(null, "RESET_LINK_OPENED", "PasswordResetToken", null, null, linkRef,
                "unauthenticated visit from emailed reset link");
        return Templates.reset_password(token, null);
    }

    @POST
    @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response resetSubmit(@FormParam("token") String token, @FormParam("password") String password) {
        if (password == null || password.length() < 10) {
            return Response.status(400).entity(Templates.reset_password(token, "Password must be at least 10 characters.")).type(MediaType.TEXT_HTML).build();
        }
        boolean ok = authService.resetPassword(token, password);
        if (!ok) {
            return Response.status(400).entity(Templates.reset_password(token, "That reset link is invalid or has expired.")).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(java.net.URI.create("/login")).build();
    }

    private String safeRedirect(String redirect, User user) {
        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return redirect;
        }
        return switch (user.role) {
            case ADMIN -> "/admin/products";
            case UNDERWRITER, SUPPORT_SPECIALIST -> "/underwriter/queue";
            case BORROWER -> "/dashboard";
        };
    }

    private jakarta.ws.rs.WebApplicationException redirectToRoleHome() {
        User user = User.findById((Long) identity.getAttribute("userId"));
        return new jakarta.ws.rs.WebApplicationException(Response.seeOther(java.net.URI.create(safeRedirect(null, user))).build());
    }
}
