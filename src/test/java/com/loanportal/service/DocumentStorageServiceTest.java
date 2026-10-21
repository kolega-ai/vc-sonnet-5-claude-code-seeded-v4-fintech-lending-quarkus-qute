package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

/** Verifies the canonicalized-base-directory containment check (precision baseline item 5). */
@QuarkusTest
class DocumentStorageServiceTest {

    @Inject
    DocumentStorageService storageService;

    @Test
    void parentDirectoryTraversalIsRejected() {
        assertThrows(ForbiddenException.class, () -> storageService.resolveForRead("../../../../etc/passwd"));
    }

    @Test
    void absoluteEscapePathIsRejected() {
        assertThrows(ForbiddenException.class, () -> storageService.resolveForRead("app-1/../../../../../etc/passwd"));
    }

    @Test
    void nonExistentButContainedPathIs404NotForbidden() {
        assertThrows(NotFoundException.class, () -> storageService.resolveForRead("app-1/does-not-exist.pdf"));
    }
}
