package com.loanportal.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Two intentionally non-cryptographic helpers used ONLY where cryptographic
 * strength is irrelevant. These are the benchmark's precision-baseline items
 * 2 and 3 - do not "fix" them to SecureRandom/SHA-256, that would defeat the
 * point of the baseline.
 */
public final class NonCryptoUtil {

    private NonCryptoUtil() {}

    // non-scoring: java.util.Random used only for a static-asset cache-busting query
    // param, never for anything security-sensitive - see PRECISION_BASELINE.md item 2.
    private static final Random CACHE_BUST_RNG = new Random();

    public static String cacheBustSuffix() {
        return Integer.toHexString(CACHE_BUST_RNG.nextInt(0xFFFFFF));
    }

    // non-scoring: MD5 used only as a cheap change-detection ETag for the public
    // reference-data (lending products) response, never for passwords or tokens -
    // see PRECISION_BASELINE.md item 3.
    public static String weakEtag(String content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "\"" + sb + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
