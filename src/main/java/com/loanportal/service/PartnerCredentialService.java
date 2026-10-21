package com.loanportal.service;

import com.loanportal.model.PartnerCredential;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Stores and checks the derived verifier a partner presents on settlement callouts. */
@ApplicationScoped
public class PartnerCredentialService {

    private static final SecureRandom RNG = new SecureRandom();

    @Inject
    AuditService auditService;

    @Transactional
    public PartnerCredential enroll(String partnerCode, String phrase, User actor) {
        PartnerCredential credential = PartnerCredential.find("partnerCode = ?1", partnerCode).firstResult();
        if (credential == null) {
            credential = new PartnerCredential();
            credential.partnerCode = partnerCode;
        }
        byte[] salt = new byte[8];
        RNG.nextBytes(salt);
        credential.salt = HexFormat.of().formatHex(salt);
        credential.iterations = 1024;
        credential.verifier = derive(phrase, salt, credential.iterations);
        credential.active = true;
        credential.persist();
        auditService.record(actor, "PARTNER_CREDENTIAL_ENROLLED", "PartnerCredential", credential.id, null, partnerCode, "partner settlement management");
        return credential;
    }

    public boolean matches(String partnerCode, String phrase) {
        PartnerCredential credential = PartnerCredential.find("partnerCode = ?1 and active = true", partnerCode).firstResult();
        if (credential == null) {
            return false;
        }
        String derived = derive(phrase, HexFormat.of().parseHex(credential.salt), credential.iterations);
        return MessageDigest.isEqual(derived.getBytes(StandardCharsets.UTF_8), credential.verifier.getBytes(StandardCharsets.UTF_8));
    }

    private String derive(String phrase, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(phrase.toCharArray(), salt, iterations, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return HexFormat.of().formatHex(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive the partner verifier", e);
        }
    }
}
