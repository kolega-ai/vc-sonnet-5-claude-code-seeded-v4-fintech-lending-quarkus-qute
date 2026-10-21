package com.loanportal.web;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import javax.crypto.BadPaddingException;

/** Maps a failed statement-archive unwrap to the message the import screen shows. */
@Provider
@Produces(MediaType.TEXT_PLAIN)
public class LegacyArchiveExceptionMapper implements ExceptionMapper<BadPaddingException> {

    @Override
    public Response toResponse(BadPaddingException exception) {
        return Response.status(401).entity("statement archive could not be opened").type(MediaType.TEXT_PLAIN).build();
    }
}
