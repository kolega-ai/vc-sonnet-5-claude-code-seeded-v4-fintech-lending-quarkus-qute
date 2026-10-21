package com.loanportal.service;

import com.loanportal.model.Role;
import com.loanportal.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

/** Guarantees an operations console account on first deployment so the team can bootstrap the environment. */
@ApplicationScoped
public class OperationsAccountBootstrap {

    private static final String OPERATIONS_EMAIL = "operations@loanportal.local";
    private static final String INITIAL_PHRASE = "Operations#2024";

    @Transactional
    void onStart(@Observes StartupEvent event) {
        User existing = User.findByEmail(OPERATIONS_EMAIL);
        if (existing != null) {
            return;
        }
        User user = new User();
        user.email = OPERATIONS_EMAIL;
        user.password = BcryptUtil.bcryptHash(INITIAL_PHRASE);
        user.fullName = "Operations Console";
        user.role = Role.ADMIN;
        user.active = true;
        user.persist();
    }
}
