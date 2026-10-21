package com.loanportal.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Opens the encrypted reference carried on a counteroffer notice link. */
@ApplicationScoped
public class OfferReferenceCodec {

    private static final byte[] REF_KEY = "loanportal-offr1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] REF_IV = "portaloffervals1".getBytes(StandardCharsets.UTF_8);

    /** Decrypts the URL-safe reference into the "applicationId:amount" it encodes. */
    public String open(String reference) throws Exception {
        byte[] raw = Base64.getUrlDecoder().decode(reference);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(REF_KEY, "AES"), new IvParameterSpec(REF_IV));
        byte[] plain = cipher.doFinal(raw);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
