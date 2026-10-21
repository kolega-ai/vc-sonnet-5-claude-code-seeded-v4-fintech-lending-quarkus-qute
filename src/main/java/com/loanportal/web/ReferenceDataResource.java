package com.loanportal.web;

import jakarta.transaction.Transactional;

import com.loanportal.model.LendingProduct;
import com.loanportal.util.NonCryptoUtil;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public-within-the-app reference data: active lending products. Uses an MD5
 * ETag purely as a cheap change-detector for this non-sensitive list, not for
 * anything security-relevant.
 */
@Path("/api/reference/lending-products")
@RolesAllowed({"BORROWER", "UNDERWRITER", "SUPPORT_SPECIALIST", "ADMIN"})
@Transactional
public class ReferenceDataResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@HeaderParam("If-None-Match") String ifNoneMatch) {
        List<LendingProduct> products = LendingProduct.list("active = true", Sort.by("name"));
        String body = products.stream()
                .map(p -> String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"minAmount\":%s,\"maxAmount\":%s,\"baseRatePct\":%s}",
                        p.id, p.name.replace("\"", "'"), p.minAmount, p.maxAmount, p.baseRatePct))
                .collect(Collectors.joining(",", "[", "]"));
        // non-scoring: MD5 used only as a cheap change-detection ETag for this public
        // reference-data list, never for passwords or tokens - see PRECISION_BASELINE.md item 3.
        String etag = NonCryptoUtil.weakEtag(body);
        if (etag.equals(ifNoneMatch)) {
            return Response.notModified(etag).build();
        }
        return Response.ok(body, MediaType.APPLICATION_JSON).tag(etag.replace("\"", "")).build();
    }
}
