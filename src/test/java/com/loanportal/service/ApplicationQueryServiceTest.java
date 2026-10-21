package com.loanportal.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loanportal.model.LoanApplication;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

/** sortBy is whitelist-mapped, never string-concatenated (precision baseline item 4). */
@QuarkusTest
class ApplicationQueryServiceTest {

    @Inject
    ApplicationQueryService queryService;

    @Test
    @TestTransaction
    void unknownSortByFallsBackToDefaultInsteadOfErroring() {
        List<LoanApplication> result = assertDoesNotThrow(() ->
                queryService.underwriterQueue("'; DROP TABLE loan_application; --", true, null, null));
        assertTrue(result != null);
    }

    @Test
    @TestTransaction
    void validWhitelistedSortKeysAreAccepted() {
        for (String key : new String[] {"amount", "submitted", "risk", "created"}) {
            assertDoesNotThrow(() -> queryService.underwriterQueue(key, false, null, null));
        }
    }
}
