package com.loanportal.web;

import jakarta.transaction.Transactional;

import com.loanportal.model.User;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
@Transactional
public class RootResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public Response index() {
        if (identity == null || identity.isAnonymous()) {
            return Response.seeOther(URI.create("/login")).build();
        }
        User user = User.findById((Long) identity.getAttribute("userId"));
        String target = switch (user.role) {
            case ADMIN -> "/admin/products";
            case UNDERWRITER, SUPPORT_SPECIALIST -> "/underwriter/queue";
            case BORROWER -> "/dashboard";
        };
        return Response.seeOther(URI.create(target)).build();
    }
}
