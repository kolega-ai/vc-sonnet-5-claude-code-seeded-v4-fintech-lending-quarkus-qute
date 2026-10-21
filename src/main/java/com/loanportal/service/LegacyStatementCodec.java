package com.loanportal.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Unwraps the AES-CBC envelope produced by the retired core-banking statement exporter. */
@ApplicationScoped
public class LegacyStatementCodec {

    private static final byte[] ARCHIVE_KEY = "loanportal-arch1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARCHIVE_IV = "portalarchiveiv1".getBytes(StandardCharsets.UTF_8);
    private static final String MAGIC = "LSA1";

    /** Decodes the Base64 envelope and returns the plaintext rows that follow the archive header. */
    public String unwrap(byte[] blob) throws Exception {
        byte[] raw = Base64.getDecoder().decode(new String(blob, StandardCharsets.UTF_8).trim());
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(ARCHIVE_KEY, "AES"), new IvParameterSpec(ARCHIVE_IV));
        byte[] plain = cipher.doFinal(raw);
        String text = new String(plain, StandardCharsets.UTF_8);
        if (!text.startsWith(MAGIC)) {
            throw new jakarta.ws.rs.WebApplicationException(
                    jakarta.ws.rs.core.Response.status(422).entity("archive format not recognized").build());
        }
        return text.substring(MAGIC.length());
    }
}
