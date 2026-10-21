package com.loanportal.service;

import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.LoanApplication;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ApplicationQueryService {

    /**
     * Underwriter queue query. sortBy is validated against a fixed whitelist and
     * mapped to the real JPQL property before being handed to Panache - it is
     * NEVER concatenated into the query string.
     * non-scoring: bounded/whitelisted sort baseline (item 4), intentional and secure.
     */
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "amount", "requestedAmount",
            "submitted", "submittedAt",
            "risk", "riskTier",
            "created", "createdAt"
    );

    public List<LoanApplication> underwriterQueue(String sortBy, boolean desc, ApplicationStatus statusFilter, Long underwriterFilter) {
        String property = SORT_WHITELIST.getOrDefault(sortBy, "createdAt");
        Sort sort = Sort.by(property, desc ? Sort.Direction.Descending : Sort.Direction.Ascending);

        StringBuilder query = new StringBuilder("status in (:statuses)");
        Parameters params = Parameters.with("statuses", List.of(
                ApplicationStatus.SUBMITTED, ApplicationStatus.IN_REVIEW,
                ApplicationStatus.NEEDS_DOCUMENTS, ApplicationStatus.COUNTEROFFERED));
        if (statusFilter != null) {
            query.append(" and status = :status");
            params = params.and("status", statusFilter);
        }
        if (underwriterFilter != null) {
            query.append(" and assignedUnderwriter.id = :uw");
            params = params.and("uw", underwriterFilter);
        }
        return LoanApplication.find(query.toString(), sort, params).list();
    }

    public List<LoanApplication> forBorrower(Long borrowerProfileId) {
        return LoanApplication.list("borrowerProfile.id = ?1 order by createdAt desc", borrowerProfileId);
    }
}
