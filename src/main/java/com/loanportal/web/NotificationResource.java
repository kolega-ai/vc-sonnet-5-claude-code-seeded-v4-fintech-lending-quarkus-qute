package com.loanportal.web;

import jakarta.transaction.Transactional;

import com.loanportal.model.Notification;
import com.loanportal.security.CsrfGuard;
import com.loanportal.security.CurrentUser;
import com.loanportal.service.NotificationService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/notifications")
@RolesAllowed({"BORROWER", "UNDERWRITER", "SUPPORT_SPECIALIST", "ADMIN"})
@Transactional
public class NotificationResource {

    @CheckedTemplate(basePath = "shared")
    static class Templates {
        static native TemplateInstance notifications(List<Notification> notifications);
    }

    @Inject CurrentUser currentUser;
    @Inject CsrfGuard csrfGuard;
    @Inject NotificationService notificationService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.notifications(notificationService.forUser(currentUser.id(), 100));
    }

    @POST
    @Path("/mark-all-read")
    public Response markAllRead(@FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        notificationService.markAllRead(currentUser.id());
        return Response.seeOther(URI.create("/notifications")).build();
    }
}
