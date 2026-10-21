package com.loanportal.service;

import com.loanportal.model.PasswordResetToken;
import com.loanportal.model.User;
import com.loanportal.security.SessionTokenService;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthService {

    @Inject
    SessionTokenService sessionTokenService;

    @Inject
    NotificationService notificationService;

    @Inject
    AuditService auditService;

    @ConfigProperty(name = "app.reset-token.ttl-minutes")
    int resetTtlMinutes;

    private static final SecureRandom RNG = new SecureRandom();

    public record LoginResult(boolean success, String sessionToken, User user) {}

    @Transactional
    public LoginResult login(String email, String rawPassword) {
        User user = User.findByEmail(email);
        // Always run bcrypt verify against a real or dummy hash to avoid a timing
        // side-channel that would reveal whether the email exists.
        String hashToCheck = (user != null) ? user.password
                : "$2a$10$abcdefghijklmnopqrstuuC7C1vXW0iC2b2b2b2b2b2b2b2b2b2b2";
        boolean ok;
        try {
            ok = BcryptUtil.matches(rawPassword, hashToCheck);
        } catch (Exception e) {
            ok = false;
        }
        if (user == null || !user.active || !ok) {
            auditService.record(null, "LOGIN_FAILURE", "User", null, null, "email=" + email, "unauthenticated");
            return new LoginResult(false, null, null);
        }
        String token = sessionTokenService.issue(user.id);
        auditService.record(user, "LOGIN_SUCCESS", "User", user.id, null, null, "credentials verified");
        return new LoginResult(true, token, user);
    }

    @Transactional
    public void logout(User user) {
        if (user != null) {
            auditService.record(user, "LOGOUT", "User", user.id, null, null, "session terminated by user");
        }
    }

    /** Always returns normally regardless of whether the email exists, to avoid user enumeration. */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = User.findByEmail(email);
        if (user == null || !user.active) {
            return;
        }
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        PasswordResetToken prt = new PasswordResetToken();
        prt.user = user;
        prt.tokenHash = sha256(rawToken);
        prt.expiresAt = Instant.now().plus(resetTtlMinutes, ChronoUnit.MINUTES);
        prt.persist();
        String link = "/reset-password?token=" + rawToken;
        notificationService.sendEmail(user.email, "Reset your Loan Origination Portal password",
                "A password reset was requested for your account. This link expires in " + resetTtlMinutes
                        + " minutes and can be used once: " + link);
        auditService.record(user, "PASSWORD_RESET_REQUESTED", "User", user.id, null, null, "self-service, email verified via outbox link");
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String hash = sha256(rawToken);
        PasswordResetToken prt = PasswordResetToken.find("tokenHash = ?1", hash).firstResult();
        if (prt == null || prt.used || prt.expiresAt.isBefore(Instant.now())) {
            return false;
        }
        prt.used = true;
        User user = prt.user;
        user.password = BcryptUtil.bcryptHash(newPassword);
        auditService.record(user, "PASSWORD_RESET_COMPLETED", "User", user.id, null, null, "single-use time-bound token verified");
        return true;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
