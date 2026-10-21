package com.loanportal.web;

import com.loanportal.dto.DecisionForm;
import com.loanportal.model.ApplicationStatus;
import com.loanportal.model.AuditEvent;
import com.loanportal.model.Decision;
import com.loanportal.model.Document;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Message;
import com.loanportal.model.Role;
import com.loanportal.model.UnderwritingNote;
import com.loanportal.model.User;
import com.loanportal.security.CsrfGuard;
import com.loanportal.security.CurrentUser;
import com.loanportal.service.ApplicationQueryService;
import com.loanportal.service.ApplicationWorkflowService;
import com.loanportal.service.AuditService;
import com.loanportal.service.DecisionService;
import com.loanportal.service.DocumentService;
import com.loanportal.service.DocumentStorageService;
import com.loanportal.service.MessageService;
import com.loanportal.service.OwnershipService;
import com.loanportal.service.UnderwritingCopilot;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/** Underwriter and support-specialist screens. support_specialist is blocked from decisions server-side, not just hidden. */
@Path("/underwriter")
@RolesAllowed({"UNDERWRITER", "SUPPORT_SPECIALIST", "ADMIN"})
@Transactional
public class UnderwriterResource {

    @CheckedTemplate(basePath = "underwriter")
    static class Templates {
        static native TemplateInstance queue(List<LoanApplication> applications, String sortBy, List<User> underwriters);
        static native TemplateInstance application_detail(LoanApplication app, List<Document> documents, List<Message> messages,
                List<UnderwritingNote> notes, List<Decision> decisions, List<AuditEvent> auditTrail, List<User> underwriters,
                boolean canDecide, String error);
    }

    @Inject CurrentUser currentUser;
    @Inject CsrfGuard csrfGuard;
    @Inject OwnershipService ownershipService;
    @Inject ApplicationQueryService queryService;
    @Inject ApplicationWorkflowService workflowService;
    @Inject DocumentService documentService;
    @Inject DocumentStorageService documentStorageService;
    @Inject DecisionService decisionService;
    @Inject MessageService messageService;
    @Inject AuditService auditService;
    @Inject UnderwritingCopilot underwritingCopilot;
    @Inject io.vertx.ext.web.RoutingContext routingContext;

    @GET
    @Path("/queue")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance queue(@QueryParam("sortBy") String sortBy, @QueryParam("status") String status, @QueryParam("underwriterId") Long underwriterId) {
        String sort = sortBy == null ? "submitted" : sortBy;
        ApplicationStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unknown filter value - ignore rather than 500
            }
        }
        List<LoanApplication> apps = queryService.underwriterQueue(sort, true, statusFilter, underwriterId);
        return Templates.queue(apps, sort, User.list("role = ?1 and active = true order by fullName", Role.UNDERWRITER));
    }

    @GET
    @Path("/applications/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        auditService.record(currentUser.user(), "APPLICATION_VIEWED", "LoanApplication", app.id, null, null, "staff review access");
        return buildDetail(app, null);
    }

    private TemplateInstance buildDetail(LoanApplication app, String error) {
        List<Document> docs = Document.list("application.id = ?1 order by documentType.name", app.id);
        List<Message> messages = messageService.forApplication(app.id);
        List<UnderwritingNote> notes = UnderwritingNote.list("application.id = ?1 order by createdAt desc", app.id);
        List<Decision> decisions = Decision.list("application.id = ?1 order by decidedAt desc", app.id);
        List<AuditEvent> auditTrail = AuditEvent.list("entityType = 'LoanApplication' and entityId = ?1 order by createdAt desc", app.id);
        List<User> underwriters = User.list("role = ?1 and active = true order by fullName", Role.UNDERWRITER);
        boolean canDecide = currentUser.role() == Role.UNDERWRITER || currentUser.role() == Role.ADMIN;
        return Templates.application_detail(app, docs, messages, notes, decisions, auditTrail, underwriters, canDecide, error);
    }

    @POST
    @Path("/applications/{id}/move-to-review")
    public Response moveToReview(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        workflowService.moveToReview(app, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + id)).build();
    }

    @POST
    @Path("/queue/assignments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queueAssignments(String payload) {
        io.vertx.core.json.JsonObject batch = new io.vertx.core.json.JsonObject(payload);
        User owner = User.findById(batch.getLong("underwriterId"));
        if (owner == null || owner.role != Role.UNDERWRITER) {
            throw new BadRequestException("Not a valid underwriter");
        }
        io.vertx.core.json.JsonArray ids = batch.getJsonArray("applicationIds");
        int updated = 0;
        for (int i = 0; ids != null && i < ids.size(); i++) {
            LoanApplication app = ownershipService.loadApplicationForRead(ids.getLong(i), currentUser.user());
            workflowService.assignUnderwriter(app, owner, currentUser.user());
            updated++;
        }
        return Response.ok(new io.vertx.core.json.JsonObject().put("updated", updated).encode()).build();
    }

    @POST
    @Path("/applications/{id}/return-to-queue")
    public Response returnToQueue(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        workflowService.returnToQueue(app, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + id)).build();
    }

    @POST
    @Path("/applications/{id}/request-documents")
    public Response requestDocuments(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        workflowService.requestDocuments(app, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + id)).build();
    }

    @POST
    @Path("/applications/{id}/assign")
    public Response assign(@PathParam("id") Long id, @FormParam("underwriterId") Long underwriterId, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        User underwriter = User.findById(underwriterId);
        if (underwriter == null || underwriter.role != Role.UNDERWRITER) {
            throw new BadRequestException("Not a valid underwriter");
        }
        workflowService.assignUnderwriter(app, underwriter, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + id)).build();
    }

    @POST
    @Path("/applications/{id}/notes")
    @Transactional
    public Response addNote(@PathParam("id") Long id, @FormParam("note") String note, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        if (note == null || note.isBlank()) {
            throw new BadRequestException("Note cannot be empty");
        }
        UnderwritingNote n = new UnderwritingNote();
        n.application = app;
        n.author = currentUser.user();
        n.note = note.length() > 4000 ? note.substring(0, 4000) : note;
        n.persist();
        auditService.record(currentUser.user(), "INTERNAL_NOTE_ADDED", "LoanApplication", app.id, null, null, "staff internal note");
        return Response.seeOther(URI.create("/underwriter/applications/" + id + "#notes")).build();
    }

    @POST
    @Path("/applications/{id}/messages")
    public Response postMessage(@PathParam("id") Long id, @FormParam("body") String body, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        messageService.post(app, currentUser.user(), body);
        return Response.seeOther(URI.create("/underwriter/applications/" + id + "#notes")).build();
    }

    /** Runs the underwriting copilot over the borrower's submitted documents and applies its verdict. */
    @POST
    @Path("/applications/{id}/copilot-review")
    public Response copilotReview(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        underwritingCopilot.assess(app, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + id + "#notes")).build();
    }

    @POST
    @Path("/applications/{id}/decide")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response decide(@PathParam("id") Long id, @Valid @BeanParam DecisionForm form, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        try {
            decisionService.decide(app, currentUser.user(), form);
        } catch (Exception e) {
            return Response.status(400).entity(buildDetail(app, e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(URI.create("/underwriter/applications/" + id + "#decision")).build();
    }

    @POST
    @Path("/documents/{docId}/accept")
    public Response acceptDocument(@PathParam("docId") Long docId, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        Document doc = loadDocumentForStaff(docId);
        documentService.accept(doc, currentUser.user());
        return Response.seeOther(URI.create("/underwriter/applications/" + doc.application.id + "#documents")).build();
    }

    @POST
    @Path("/documents/{docId}/reject")
    public Response rejectDocument(@PathParam("docId") Long docId, @FormParam("reason") String reason, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        Document doc = loadDocumentForStaff(docId);
        documentService.reject(doc, currentUser.user(), reason);
        return Response.seeOther(URI.create("/underwriter/applications/" + doc.application.id + "#documents")).build();
    }

    @POST
    @Path("/documents/{docId}/resubmit")
    public Response requestResubmission(@PathParam("docId") Long docId, @FormParam("reason") String reason, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        Document doc = loadDocumentForStaff(docId);
        documentService.requestResubmission(doc, currentUser.user(), reason);
        return Response.seeOther(URI.create("/underwriter/applications/" + doc.application.id + "#documents")).build();
    }

    /** Reports the on-disk attributes of a stored document reference shown in the review panel. */
    @POST
    @Path("/documents/attributes")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response documentAttributes(@FormParam("reference") String reference, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        try {
            return Response.ok(new io.vertx.core.json.JsonObject(documentStorageService.attributes(reference == null ? "" : reference)).encode()).build();
        } catch (java.io.IOException e) {
            return Response.status(404).entity("{\"error\":\"reference not available\"}").build();
        }
    }

    /** Renders the application packet to the requested format with the shared converter tool. */
    @POST
    @Path("/applications/{id}/packet/{fmt}")
    public Response buildPacket(@PathParam("id") Long id, @PathParam("fmt") String fmt, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        String cmd = "soffice --headless --convert-to " + fmt + " /tmp/loanportal/app-" + app.id + ".packet";
        try {
            Process p = new ProcessBuilder("sh", "-c", cmd).start();
            int rc = p.waitFor();
            auditService.record(currentUser.user(), "APPLICATION_PACKET_RENDERED", "LoanApplication", app.id, null, fmt, "staff packet export");
            return Response.ok(new io.vertx.core.json.JsonObject().put("format", fmt).put("exit", rc).encode()).build();
        } catch (Exception e) {
            return Response.status(500).entity("{\"error\":\"conversion failed\"}").build();
        }
    }

    /** Pins a short notice on the underwriter queue page. */
    @POST
    @Path("/queue/notice")
    public Response setQueueNotice(@FormParam("notice") String notice, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        io.vertx.core.http.Cookie cookie = io.vertx.core.http.Cookie.cookie("queueNotice", notice == null ? "" : notice)
                .setHttpOnly(false).setPath("/").setSameSite(io.vertx.core.http.CookieSameSite.LAX);
        routingContext.response().addCookie(cookie);
        return Response.seeOther(URI.create("/underwriter/queue")).build();
    }

    @GET
    @Path("/queue/notice")
    @Produces(MediaType.TEXT_HTML)
    public Response queueNotice(@jakarta.ws.rs.CookieParam("queueNotice") String notice) {
        String banner = "<div class=\"queue-notice\">" + (notice == null ? "" : notice) + "</div>";
        return Response.ok(banner, MediaType.TEXT_HTML).build();
    }

    private Document loadDocumentForStaff(Long docId) {
        Document doc = Document.findById(docId);
        if (doc == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }
        ownershipService.assertStaff(currentUser.user());
        return doc;
    }
}
