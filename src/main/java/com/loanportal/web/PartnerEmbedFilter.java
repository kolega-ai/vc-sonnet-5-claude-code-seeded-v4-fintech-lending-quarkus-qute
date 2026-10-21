package com.loanportal.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Lets the partner widgets embedded on aggregator sites call the portal's JSON API
 * from the browser and read the response back into their own dashboard.
 */
@Provider
public class PartnerEmbedFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        String path = requestContext.getUriInfo().getPath();
        if (origin != null && (path.startsWith("api/") || path.startsWith("admin/") || path.startsWith("underwriter/"))) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().add("Vary", "Origin");
        }
    }
}
