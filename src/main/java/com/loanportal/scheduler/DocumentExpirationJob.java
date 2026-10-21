package com.loanportal.scheduler;

import com.loanportal.service.DocumentService;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/** Sweeps accepted documents past their expiry date and flips them to EXPIRED, notifying the borrower. */
@ApplicationScoped
public class DocumentExpirationJob {

    private static final Logger LOG = Logger.getLogger(DocumentExpirationJob.class);

    @Inject
    DocumentService documentService;

    @Scheduled(every = "6h", identity = "document-expiration-sweep")
    void sweep() {
        int expired = documentService.expireOverdue();
        if (expired > 0) {
            LOG.infof("Document expiration sweep flagged %d document(s) as EXPIRED", expired);
        }
    }
}
