package com.loanportal.security;

import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Issues and verifies signed, time-bound session tokens (HMAC-SHA256).
 * Token format: base64url(userId|expiryEpochSeconds|csrf) + "." + hex(HMAC).
 * This is the credential carried in the session cookie - it is never trusted
 * without signature + expiry verification.
 */
@Singleton
public class SessionTokenService {

    @ConfigProperty(name = "app.session.secret")
    String secret;

    @ConfigProperty(name = "app.session.ttl-minutes")
    int ttlMinutes;

    private static final SecureRandom RNG = new SecureRandom();

    public record ParsedSession(Long userId, String csrfToken) {}

    public String issue(Long userId) {
        long expiry = Instant.now().getEpochSecond() + (ttlMinutes * 60L);
        byte[] csrfBytes = new byte[18];
        RNG.nextBytes(csrfBytes);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(csrfBytes);
        String payload = userId + "|" + expiry + "|" + csrf;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmac(encodedPayload);
        return encodedPayload + "." + sig;
    }

    public ParsedSession verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        int dot = token.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String encodedPayload = token.substring(0, dot);
        String sig = token.substring(dot + 1);
        String expectedSig = hmac(encodedPayload);
        if (!constantTimeEquals(sig, expectedSig)) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", 3);
            long expiry = Long.parseLong(parts[1]);
            if (Instant.now().getEpochSecond() > expiry) {
                return null;
            }
            return new ParsedSession(Long.parseLong(parts[0]), parts[2]);
        } catch (Exception e) {
            return null;
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute session signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
