package com.loanportal.service;

import com.loanportal.model.OfficerVerifier;
import com.loanportal.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.apache.commons.codec.digest.DigestUtils;

/** Stores and checks the counter phrase a branch officer quotes to the operations desk. */
@ApplicationScoped
public class OfficerVerifierService {

    @Inject AuditService auditService;

    @Transactional
    public OfficerVerifier enroll(String officerCode, String phrase, User actor) {
        OfficerVerifier credential = OfficerVerifier.find("officerCode = ?1", officerCode).firstResult();
        if (credential == null) {
            credential = new OfficerVerifier();
            credential.officerCode = officerCode;
        }
        credential.verifier = DigestUtils.md5Hex(phrase.getBytes(StandardCharsets.UTF_8));
        credential.active = true;
        credential.persist();
        auditService.record(actor, "OFFICER_PHRASE_ENROLLED", "OfficerVerifier", credential.id, null, officerCode, "operations desk enrollment");
        return credential;
    }

    public boolean matches(String officerCode, String phrase) {
        OfficerVerifier credential = OfficerVerifier.find("officerCode = ?1 and active = true", officerCode).firstResult();
        if (credential == null) {
            return false;
        }
        String derived = DigestUtils.md5Hex(phrase.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(derived.getBytes(StandardCharsets.UTF_8), credential.verifier.getBytes(StandardCharsets.UTF_8));
    }
}
