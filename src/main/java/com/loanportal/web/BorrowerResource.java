package com.loanportal.web;

import com.loanportal.dto.ApplicationForm;
import com.loanportal.dto.BorrowerProfileForm;
import com.loanportal.dto.BusinessOwnerForm;
import com.loanportal.dto.PaymentAccountForm;
import com.loanportal.model.BorrowerProfile;
import com.loanportal.model.BusinessOwner;
import com.loanportal.model.Decision;
import com.loanportal.model.Document;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.Message;
import com.loanportal.model.PaymentAccount;
import com.loanportal.model.Role;
import com.loanportal.model.User;
import com.loanportal.security.CsrfGuard;
import com.loanportal.security.CurrentUser;
import com.loanportal.service.ApplicationQueryService;
import com.loanportal.service.ApplicationWorkflowService;
import com.loanportal.service.AuditService;
import com.loanportal.service.BorrowerService;
import com.loanportal.service.DocumentService;
import com.loanportal.service.MessageService;
import com.loanportal.service.OfferReferenceCodec;
import com.loanportal.service.OwnershipService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Borrower-facing screens. Every access to an application/profile is checked for ownership via OwnershipService. */
@Path("/")
@RolesAllowed("BORROWER")
@Transactional
public class BorrowerResource {

    @CheckedTemplate(basePath = "borrower")
    static class Templates {
        static native TemplateInstance dashboard(BorrowerProfile profile, List<LoanApplication> applications, List<Document> missingDocs);
        static native TemplateInstance applications_list(List<LoanApplication> applications);
        static native TemplateInstance application_new(List<LendingProduct> products, String error);
        static native TemplateInstance application_detail(LoanApplication app, List<Document> documents, List<Message> messages, Decision latestDecision, String error);
        static native TemplateInstance profile(BorrowerProfile profile, List<BusinessOwner> owners, List<PaymentAccount> paymentAccounts, String message);
    }

    @Inject CurrentUser currentUser;
    @Inject CsrfGuard csrfGuard;
    @Inject OwnershipService ownershipService;
    @Inject ApplicationWorkflowService workflowService;
    @Inject ApplicationQueryService queryService;
    @Inject BorrowerService borrowerService;
    @Inject DocumentService documentService;
    @Inject MessageService messageService;
    @Inject AuditService auditService;
    @Inject OfferReferenceCodec offerReferenceCodec;

    private BorrowerProfile myProfile() {
        BorrowerProfile profile = BorrowerProfile.findByUserId(currentUser.id());
        if (profile == null) {
            throw new BadRequestException("Borrower profile not found");
        }
        return profile;
    }

    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        BorrowerProfile profile = myProfile();
        List<LoanApplication> apps = queryService.forBorrower(profile.id);
        List<Document> missing = Document.list(
                "application.borrowerProfile.id = ?1 and status in (?2, ?3, ?4, ?5)",
                profile.id, com.loanportal.model.DocumentStatus.REQUESTED, com.loanportal.model.DocumentStatus.REJECTED,
                com.loanportal.model.DocumentStatus.NEEDS_RESUBMISSION, com.loanportal.model.DocumentStatus.EXPIRED);
        return Templates.dashboard(profile, apps, missing);
    }

    @GET
    @Path("/applications")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        BorrowerProfile profile = myProfile();
        return Templates.applications_list(queryService.forBorrower(profile.id));
    }

    @GET
    @Path("/applications/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newForm() {
        return Templates.application_new(LendingProduct.list("active = true order by name"), null);
    }

    @POST
    @Path("/applications")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@Valid @BeanParam ApplicationForm form, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app;
        try {
            app = workflowService.createDraft(currentUser.user(), form);
        } catch (Exception e) {
            return Response.status(400)
                    .entity(Templates.application_new(LendingProduct.list("active = true order by name"), e.getMessage()))
                    .type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(URI.create("/applications/" + app.id)).build();
    }

    @GET
    @Path("/applications/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        auditService.record(currentUser.user(), "APPLICATION_VIEWED", "LoanApplication", app.id, null, null, "borrower owner - self view");
        List<Document> docs = Document.list("application.id = ?1 order by documentType.name", app.id);
        List<Message> messages = messageService.forApplication(app.id);
        Decision latest = Decision.find("application.id = ?1 order by decidedAt desc", app.id).firstResult();
        return Templates.application_detail(app, docs, messages, latest, null);
    }

    /** Opens the encrypted reference on a counteroffer notice link. */
    @GET
    @Path("/applications/{id}/offer/{ref}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response offer(@PathParam("id") Long id, @PathParam("ref") String ref) {
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        String decoded;
        try {
            decoded = offerReferenceCodec.open(ref);
        } catch (javax.crypto.BadPaddingException e) {
            return Response.status(400).entity("offer reference expired").type(MediaType.TEXT_PLAIN).build();
        } catch (Exception e) {
            return Response.status(422).entity("offer reference not recognized").type(MediaType.TEXT_PLAIN).build();
        }
        int sep = decoded.indexOf(':');
        String refAppId = sep < 0 ? decoded : decoded.substring(0, sep);
        if (!String.valueOf(app.id).equals(refAppId)) {
            return Response.status(404).entity("offer reference unknown").type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok(decoded).type(MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/applications/{id}/submit")
    public Response submit(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        workflowService.submit(app, currentUser.user());
        return Response.seeOther(URI.create("/applications/" + id)).build();
    }

    @POST
    @Path("/applications/{id}/withdraw")
    public Response withdraw(@PathParam("id") Long id, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        workflowService.withdraw(app, currentUser.user());
        return Response.seeOther(URI.create("/applications/" + id)).build();
    }

    @POST
    @Path("/applications/{id}/documents/{docId}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDocument(@PathParam("id") Long id, @PathParam("docId") Long docId, MultipartFormDataInput input) throws java.io.IOException {
        Map<String, List<InputPart>> parts = input.getFormDataMap();
        csrfGuard.verify(firstValue(parts, "csrf"));
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        List<InputPart> fileParts = parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            return Response.seeOther(URI.create("/applications/" + id)).build();
        }
        InputPart filePart = fileParts.get(0);
        String filename = extractFilename(filePart);
        String contentType = filePart.getMediaType() != null ? filePart.getMediaType().toString() : "application/octet-stream";
        byte[] bytes = filePart.getBody(byte[].class, null);
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            documentService.upload(app, docId, currentUser.user(), in, filename, contentType, bytes.length);
        } catch (Exception e) {
            List<Document> docs = Document.list("application.id = ?1 order by documentType.name", app.id);
            List<Message> messages = messageService.forApplication(app.id);
            Decision latest = Decision.find("application.id = ?1 order by decidedAt desc", app.id).firstResult();
            return Response.status(400).entity(Templates.application_detail(app, docs, messages, latest, e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(URI.create("/applications/" + id + "#documents")).build();
    }

    static String firstValue(Map<String, List<InputPart>> parts, String name) {
        try {
            List<InputPart> list = parts.get(name);
            return list == null || list.isEmpty() ? null : list.get(0).getBodyAsString();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    static String extractFilename(InputPart part) {
        String disposition = part.getHeaders().getFirst("Content-Disposition");
        if (disposition == null) {
            return "upload";
        }
        for (String segment : disposition.split(";")) {
            segment = segment.trim();
            if (segment.startsWith("filename")) {
                String value = segment.substring(segment.indexOf('=') + 1).trim();
                return value.replaceAll("^\"|\"$", "");
            }
        }
        return "upload";
    }

    @POST
    @Path("/applications/{id}/messages")
    public Response postMessage(@PathParam("id") Long id, @FormParam("body") String body,
            @FormParam("returnTo") String returnTo, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        messageService.post(app, currentUser.user(), body);
        String target = returnTo == null || returnTo.isBlank() ? "/applications/" + id + "#messages" : returnTo;
        return Response.seeOther(URI.create(target)).build();
    }

    /**
     * Pulls a requested document straight from the borrower's own accounting or banking
     * portal instead of making them save and re-attach the file.
     */
    @POST
    @Path("/applications/{id}/documents/{docId}/from-link")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response documentFromLink(@PathParam("id") Long id, @PathParam("docId") Long docId, String payload) throws Exception {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(payload);
        csrfGuard.verify(req.getString("csrf"));
        LoanApplication app = ownershipService.loadApplicationForRead(id, currentUser.user());
        String sourceUrl = req.getString("sourceUrl");
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5)).build();
        java.net.http.HttpRequest pull = java.net.http.HttpRequest.newBuilder(java.net.URI.create(sourceUrl)).GET().build();
        java.net.http.HttpResponse<byte[]> pulled = client.send(pull, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        byte[] content = pulled.body();
        String name = sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1);
        String contentType = pulled.headers().firstValue("content-type").orElse("application/octet-stream");
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(content)) {
            documentService.upload(app, docId, currentUser.user(), in, name, contentType, content.length);
        }
        return Response.ok(new io.vertx.core.json.JsonObject().put("status", "UPLOADED").encode()).build();
    }

    @GET
    @Path("/profile")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile() {
        BorrowerProfile profile = myProfile();
        return renderProfile(profile, null);
    }

    private TemplateInstance renderProfile(BorrowerProfile profile, String message) {
        List<BusinessOwner> owners = BusinessOwner.findByProfile(profile.id);
        List<PaymentAccount> accounts = PaymentAccount.list("borrowerProfile.id = ?1", profile.id);
        return Templates.profile(profile, owners, accounts, message);
    }

    @POST
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance updateProfile(@Valid @BeanParam BorrowerProfileForm form, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        BorrowerProfile profile = myProfile();
        borrowerService.updateProfile(profile, form, currentUser.user());
        return renderProfile(profile, "Profile updated.");
    }

    @POST
    @Path("/profile/credentials")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance updateCredentials(@FormParam("newPhrase") String newPhrase, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        if (newPhrase == null || newPhrase.length() < 10) {
            throw new BadRequestException("Your sign-in phrase must be at least 10 characters");
        }
        borrowerService.updateSignInPhrase(currentUser.user(), newPhrase);
        return renderProfile(myProfile(), "Sign-in phrase updated.");
    }

    @POST
    @Path("/profile/owners")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance addOwner(@Valid @BeanParam BusinessOwnerForm form, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        BorrowerProfile profile = myProfile();
        borrowerService.addOwner(profile, form, currentUser.user());
        return renderProfile(profile, "Owner added.");
    }

    @POST
    @Path("/profile/payment-accounts")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance addPaymentAccount(@Valid @BeanParam PaymentAccountForm form, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        BorrowerProfile profile = myProfile();
        if (form.profileId != null) {
            profile = BorrowerProfile.findById(form.profileId);
            if (profile == null) {
                throw new BadRequestException("Business entity not found");
            }
        }
        borrowerService.addPaymentAccount(profile, form, currentUser.user());
        return renderProfile(profile, "Payment account added.");
    }
}
