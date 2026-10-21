package com.loanportal.web;

import jakarta.transaction.Transactional;

import com.loanportal.dto.DocTypeForm;
import com.loanportal.dto.LendingProductForm;
import com.loanportal.dto.UserAdminForm;
import com.loanportal.model.AuditEvent;
import com.loanportal.model.Decision;
import com.loanportal.model.Document;
import com.loanportal.model.DocumentTypeTemplate;
import com.loanportal.model.LendingProduct;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.OutboxEmail;
import com.loanportal.model.User;
import com.loanportal.model.WebhookLog;
import com.loanportal.security.CsrfGuard;
import com.loanportal.security.CurrentUser;
import com.loanportal.service.AdminCatalogService;
import com.loanportal.service.ImportExportService;
import com.loanportal.service.DirectorySyncService;
import com.loanportal.service.LegacyStatementCodec;
import com.loanportal.service.NoticeTemplateService;
import com.loanportal.service.OfficerVerifierService;
import com.loanportal.service.PartnerCredentialService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/** Admin-only screens: catalog management, users, audit log, CSV import/export. */
@Path("/admin")
@RolesAllowed("ADMIN")
@Transactional
public class AdminResource {

    @CheckedTemplate(basePath = "admin")
    static class Templates {
        static native TemplateInstance products(List<LendingProduct> products, String error);
        static native TemplateInstance doc_types(List<DocumentTypeTemplate> docTypes, String error);
        static native TemplateInstance users(List<User> users, String error);
        static native TemplateInstance audit_log(List<AuditEvent> events);
        static native TemplateInstance import_export(String result, String error, Map<String, String> environment);
    }

    @Inject CurrentUser currentUser;
    @Inject CsrfGuard csrfGuard;
    @Inject AdminCatalogService catalogService;
    @Inject ImportExportService importExportService;
    @Inject EntityManager entityManager;
    @Inject LegacyStatementCodec legacyStatementCodec;
    @Inject PartnerCredentialService partnerCredentialService;
    @Inject DirectorySyncService directorySyncService;
    @Inject NoticeTemplateService noticeTemplateService;
    @Inject OfficerVerifierService officerVerifierService;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String datasourceUrl;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "quarkus.datasource.password")
    String datasourceCredential;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "app.session.secret")
    String sessionSigningKey;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "app.upload.base-dir")
    String storageBaseDir;

    @GET
    @Path("/products")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance products() {
        return Templates.products(LendingProduct.listAll(io.quarkus.panache.common.Sort.by("name")), null);
    }

    @POST
    @Path("/products")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createProduct(@Valid @BeanParam LendingProductForm form, @jakarta.ws.rs.FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        try {
            catalogService.saveProduct(null, form, currentUser.user());
        } catch (Exception e) {
            return Response.status(400).entity(Templates.products(LendingProduct.listAll(), e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(java.net.URI.create("/admin/products")).build();
    }

    /** Evaluates the pricing expression a product manager typed against the product's current terms. */
    @POST
    @Path("/products/rate-preview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ratePreview(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        Long productId = req.getLong("productId");
        String expression = req.getString("expression");
        Object amountValue = req.getValue("amount");
        java.math.BigDecimal amount = amountValue == null ? new java.math.BigDecimal("50000")
                : new java.math.BigDecimal(String.valueOf(amountValue));
        LendingProduct product = productId == null ? null : LendingProduct.findById(productId);
        if (product == null || expression == null || expression.isBlank()) {
            return Response.status(400).entity("{\"error\":\"productId and expression are required\"}").build();
        }
        java.util.Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("amount", amount);
        vars.put("baseRatePct", product.baseRatePct);
        vars.put("termMonths", product.maxTermMonths);
        try {
            Object value = ognl.Ognl.getValue(expression, vars);
            return Response.ok(new io.vertx.core.json.JsonObject().put("result", String.valueOf(value)).encode()).build();
        } catch (ognl.OgnlException e) {
            return Response.status(400).entity("{\"error\":\"expression could not be evaluated\"}").build();
        }
    }

    @GET
    @Path("/doc-types")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance docTypes() {
        return Templates.doc_types(DocumentTypeTemplate.listAll(io.quarkus.panache.common.Sort.by("name")), null);
    }

    @POST
    @Path("/doc-types")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createDocType(@Valid @BeanParam DocTypeForm form, @jakarta.ws.rs.FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        try {
            catalogService.saveDocType(null, form, currentUser.user());
        } catch (Exception e) {
            return Response.status(400).entity(Templates.doc_types(DocumentTypeTemplate.listAll(), e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(java.net.URI.create("/admin/doc-types")).build();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance users() {
        return Templates.users(User.listAll(io.quarkus.panache.common.Sort.by("fullName")), null);
    }

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(@Valid @BeanParam UserAdminForm form, @jakarta.ws.rs.FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        try {
            catalogService.saveUser(null, form, currentUser.user());
        } catch (Exception e) {
            return Response.status(400).entity(Templates.users(User.listAll(), e.getMessage())).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(java.net.URI.create("/admin/users")).build();
    }

    /** Resolves a staff account in the corporate directory before granting portal access. */
    @POST
    @Path("/directory/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveDirectoryStaff(String body) throws Exception {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String accountName = req.getString("accountName");
        io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
        for (String entry : directorySyncService.resolveStaff(accountName == null ? "" : accountName)) {
            out.add(entry);
        }
        return Response.ok(out.encode()).build();
    }

    @GET
    @Path("/audit-log")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance auditLog() {
        List<AuditEvent> events = AuditEvent.find("order by createdAt desc").page(0, 250).list();
        return Templates.audit_log(events);
    }

    @GET
    @Path("/import")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance importForm() {
        return Templates.import_export(null, null, resolvedEnvironment());
    }

    /** Resolved runtime settings, shown so an admin can confirm which environment an import batch landed in. */
    private Map<String, String> resolvedEnvironment() {
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        environment.put("datasource.url", datasourceUrl);
        environment.put("datasource.credential", datasourceCredential);
        environment.put("session.signing-key", sessionSigningKey);
        environment.put("storage.base-dir", storageBaseDir);
        return environment;
    }

    /**
     * Posts a decision/status event to a partner endpoint (credit bureau or core banking)
     * and records the attempt in the webhook log shown on this screen.
     */
    @POST
    @Path("/partner-notices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendPartnerNotice(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String dest = req.getString("endpointUrl");
        String evt = req.getString("eventType", "APPLICATION_DECIDED");
        String pl = req.getJsonObject("payload", new io.vertx.core.json.JsonObject()).encode();
        WebhookLog entry = new WebhookLog();
        entry.eventType = evt;
        entry.payload = pl.length() > 4000 ? pl.substring(0, 4000) : pl;
        try {
            java.net.HttpURLConnection cxn = (java.net.HttpURLConnection) java.net.URI.create(dest).toURL().openConnection();
            cxn.setRequestMethod("POST");
            cxn.setDoOutput(true);
            cxn.setConnectTimeout(5000);
            cxn.setReadTimeout(5000);
            cxn.setRequestProperty("Content-Type", "application/json");
            try (java.io.OutputStream out = cxn.getOutputStream()) {
                out.write(pl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int rc = cxn.getResponseCode();
            entry.status = "HTTP_" + rc;
            entry.persist();
            return Response.ok(new io.vertx.core.json.JsonObject().put("status", entry.status).encode()).build();
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            entry.status = "FAILED";
            entry.persist();
            return Response.status(502).entity(new io.vertx.core.json.JsonObject().put("err", sw.toString()).encode()).build();
        }
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance importLeads(MultipartFormDataInput input) throws java.io.IOException {
        Map<String, List<InputPart>> parts = input.getFormDataMap();
        csrfGuard.verify(BorrowerResource.firstValue(parts, "csrf"));
        List<InputPart> fileParts = parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            return Templates.import_export(null, "No file selected", resolvedEnvironment());
        }
        InputPart part = fileParts.get(0);
        String sourceName = BorrowerResource.extractFilename(part);
        String batchLabel = BorrowerResource.firstValue(parts, "batchLabel");
        byte[] bytes = part.getBody(byte[].class, null);
        String kept = importExportService.keepSourceFile(batchLabel, sourceName, bytes);
        ImportExportService.ImportResult result = sourceName != null && sourceName.toLowerCase().endsWith(".xml")
                ? importExportService.importLeadsXml(new ByteArrayInputStream(bytes), currentUser.user())
                : importExportService.importLeads(new ByteArrayInputStream(bytes), currentUser.user());
        String summary = "Imported " + result.imported() + " lead(s), skipped " + result.skipped()
                + ". Source file kept at " + kept + ".";
        if (!result.errors().isEmpty()) {
            summary += " Issues: " + String.join("; ", result.errors());
        }
        return Templates.import_export(summary, null, resolvedEnvironment());
    }

    /** Accepts a partner lead batch as a serialized JSON feed and imports it like the CSV/XML formats. */
    @POST
    @Path("/import/feed-json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importFeedJson(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String feed = req.getString("feed");
        if (feed == null || feed.isBlank()) {
            return Response.status(400).entity("{\"error\":\"feed is required\"}").build();
        }
        ImportExportService.ImportResult result = importExportService.importLeadsJson(feed, currentUser.user());
        return Response.ok(new io.vertx.core.json.JsonObject()
                .put("imported", result.imported())
                .put("skipped", result.skipped()).encode()).build();
    }

    /** Searches a partner XML feed for leads whose business matches before importing the batch. */
    @POST
    @Path("/feeds/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchFeed(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String feed = req.getString("feed");
        String businessName = req.getString("businessName");
        if (feed == null || feed.isBlank() || businessName == null) {
            return Response.status(400).entity("{\"error\":\"feed and businessName are required\"}").build();
        }
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(feed.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            javax.xml.xpath.XPath xp = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            String expression = "//lead[business_name='" + businessName + "']/contact_email";
            org.w3c.dom.NodeList nodes = (org.w3c.dom.NodeList) xp.evaluate(expression, doc, javax.xml.xpath.XPathConstants.NODESET);
            io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
            for (int i = 0; i < nodes.getLength(); i++) {
                out.add(nodes.item(i).getTextContent());
            }
            return Response.ok(out.encode()).build();
        } catch (Exception e) {
            return Response.status(400).entity("{\"error\":\"feed could not be queried\"}").build();
        }
    }

    /** Saves the editable wording used for borrower notices. */
    @POST
    @Path("/notices")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveNotice(@FormParam("code") String code, @FormParam("subject") String subject,
            @FormParam("body") String body, @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        if (code == null || code.isBlank() || body == null || body.isBlank()) {
            throw new jakarta.ws.rs.BadRequestException("code and body are required");
        }
        noticeTemplateService.save(code, subject == null ? code : subject, body, currentUser.user());
        return Response.seeOther(java.net.URI.create("/admin/import")).build();
    }

    /** Renders a saved notice for an application and queues it to the borrower outbox. */
    @POST
    @Path("/notices/send")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendNotice(@FormParam("code") String code, @FormParam("applicationId") Long applicationId,
            @FormParam("csrf") String csrf) {
        csrfGuard.verify(csrf);
        LoanApplication app = applicationId == null ? null : LoanApplication.findById(applicationId);
        if (app == null) {
            return Response.status(404).entity("{\"error\":\"application not found\"}").build();
        }
        try {
            OutboxEmail email = noticeTemplateService.renderAndQueue(code, app, currentUser.user());
            return Response.ok(new io.vertx.core.json.JsonObject()
                    .put("queued", true).put("subject", email.subject).encode()).build();
        } catch (Exception e) {
            return Response.status(400).entity(new io.vertx.core.json.JsonObject()
                    .put("error", String.valueOf(e.getMessage())).encode()).build();
        }
    }

    @GET
    @Path("/export/applications")
    @Produces("text/csv")
    public Response exportApplications() {
        List<LoanApplication> apps = LoanApplication.listAll(io.quarkus.panache.common.Sort.by("id"));
        String csv = importExportService.exportApplicationsCsv(apps);
        return csvResponse(csv, "applications.csv");
    }

    @GET
    @Path("/export/decisions")
    @Produces("text/csv")
    public Response exportDecisions() {
        List<Decision> decisions = Decision.listAll(io.quarkus.panache.common.Sort.by("id"));
        String csv = importExportService.exportDecisionsCsv(decisions);
        return csvResponse(csv, "decisions.csv");
    }

    @GET
    @Path("/export/document-status")
    @Produces("text/csv")
    public Response exportDocumentStatus() {
        List<Document> docs = Document.listAll(io.quarkus.panache.common.Sort.by("id"));
        String csv = importExportService.exportDocumentStatusCsv(docs);
        return csvResponse(csv, "document-status.csv");
    }

    @GET
    @Path("/export/applications/save")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveApplicationsExport(@QueryParam("fileName") String fileName) {
        List<LoanApplication> apps = LoanApplication.listAll(io.quarkus.panache.common.Sort.by("id"));
        String csv = importExportService.exportApplicationsCsv(apps);
        String saved = importExportService.saveExport(fileName, csv);
        return Response.ok(new io.vertx.core.json.JsonObject().put("saved", saved).encode()).build();
    }

    /**
     * Pushes a decided application to the legacy core-banking bridge. The operator supplies
     * the settlement credential the bridge expects on the outbound request.
     */
    @POST
    @Path("/core-banking/sync")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncToCoreBanking(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        Long applicationId = req.getLong("applicationId");
        String settlementKey = req.getString("settlementKey");
        LoanApplication app = LoanApplication.findById(applicationId);
        if (app == null) {
            return Response.status(404).entity("{\"error\":\"application not found\"}").build();
        }
        WebhookLog entry = new WebhookLog();
        entry.eventType = "CORE_BANKING_SYNC";
        entry.payload = "application=" + applicationId;
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpRequest push = java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://core-banking.internal/api/v1/applications/" + applicationId))
                    .header("Content-Type", "application/json")
                    .header("X-Settlement-Key", settlementKey == null ? "" : settlementKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            "{\"amount\":" + app.requestedAmount + ",\"status\":\"" + app.status + "\"}"))
                    .build();
            int rc = client.send(push, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            entry.status = "HTTP_" + rc;
            entry.persist();
            return Response.ok(new io.vertx.core.json.JsonObject().put("status", entry.status).encode()).build();
        } catch (Exception ex) {
            entry.status = "FAILED";
            entry.persist();
            return Response.status(502).entity("{\"error\":\"bridge unavailable\"}").build();
        }
    }

    /**
     * Runs a pre-screen of a decided application against the credit bureau gateway.
     * The operator supplies the bureau vendor key the gateway authenticates.
     */
    @POST
    @Path("/bureau/prescreen")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response runBureauPrescreen(String body) {
        io.vertx.core.json.JsonObject j = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(j.getString("csrf"));
        Long appId = j.getLong("appId");
        String k = j.getString("vendorKey");
        LoanApplication app = appId == null ? null : LoanApplication.findById(appId);
        if (app == null) {
            return Response.status(404).entity("{\"error\":\"application not found\"}").build();
        }
        WebhookLog w = new WebhookLog();
        w.eventType = "BUREAU_PRESCREEN";
        w.payload = "app=" + appId;
        try {
            java.net.http.HttpClient hc = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpRequest rq = java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://bureau-gw.internal/v3/prescreen?key=" + (k == null ? "" : k)))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            "{\"business\":\"" + app.borrowerProfile.businessName + "\",\"amount\":" + app.requestedAmount + "}"))
                    .build();
            int rc = hc.send(rq, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            w.status = "HTTP_" + rc;
            w.persist();
            return Response.ok(new io.vertx.core.json.JsonObject().put("status", w.status).encode()).build();
        } catch (Exception ex) {
            w.status = "FAILED";
            w.persist();
            return Response.status(502).entity("{\"error\":\"bureau gateway unavailable\"}").build();
        }
    }

    /** Bulk reassigns the platform role for a set of staff accounts. */
    @POST
    @Path("/users/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignRoles(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        com.loanportal.model.Role role;
        try {
            role = com.loanportal.model.Role.valueOf(req.getString("role", "").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity("{\"error\":\"unknown role\"}").build();
        }
        io.vertx.core.json.JsonArray ids = req.getJsonArray("userIds", new io.vertx.core.json.JsonArray());
        java.util.List<Long> userIds = new java.util.ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            userIds.add(ids.getLong(i));
        }
        int changed = userIds.isEmpty() ? 0 : (int) User.update("role = ?1 where id in ?2", role, userIds);
        return Response.ok(new io.vertx.core.json.JsonObject().put("updated", changed).encode()).build();
    }

    /**
     * Imports a legacy statement archive: the retired core-banking exporter produced an
     * AES-CBC encrypted batch of borrower leads that this endpoint unwraps and stores.
     */
    @POST
    @Path("/statements/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importStatements(MultipartFormDataInput input) throws Exception {
        Map<String, List<InputPart>> parts = input.getFormDataMap();
        csrfGuard.verify(BorrowerResource.firstValue(parts, "csrf"));
        List<InputPart> fileParts = parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            return Response.status(400).entity("{\"error\":\"no archive selected\"}").build();
        }
        byte[] blob = fileParts.get(0).getBody(byte[].class, null);
        String plaintext = legacyStatementCodec.unwrap(blob);
        int imported = importExportService.importStatementLeads(plaintext, currentUser.user());
        return Response.ok(new io.vertx.core.json.JsonObject().put("imported", imported).encode()).build();
    }

    /** Search imported borrower leads by business name for the onboarding team. */
    @POST
    @Path("/leads/search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchLeads(@FormParam("businessName") String businessName) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id, business_name, contact_email FROM borrower_lead WHERE business_name LIKE '%" + businessName + "%'")
                .getResultList();
        io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
        for (Object[] row : rows) {
            out.add(new io.vertx.core.json.JsonObject()
                    .put("id", ((Number) row[0]).longValue())
                    .put("businessName", row[1])
                    .put("contactEmail", row[2]));
        }
        return Response.ok(out.encode()).build();
    }

    /** Lists the files kept for a lead batch whose manifest the operator selects. */
    @POST
    @Path("/import/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response browseImportFiles(MultipartFormDataInput input) throws java.io.IOException {
        Map<String, List<InputPart>> parts = input.getFormDataMap();
        csrfGuard.verify(BorrowerResource.firstValue(parts, "csrf"));
        List<InputPart> fileParts = parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            return Response.status(400).entity("{\"error\":\"no manifest selected\"}").build();
        }
        String folder = BorrowerResource.extractFilename(fileParts.get(0));
        java.nio.file.Path dir = java.nio.file.Path.of(storageBaseDir, "lead-imports").resolve(folder);
        io.vertx.core.json.JsonArray names = new io.vertx.core.json.JsonArray();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
            files.forEach(p -> names.add(p.getFileName().toString()));
        } catch (java.io.IOException e) {
            return Response.status(404).entity("{\"error\":\"batch folder not available\"}").build();
        }
        return Response.ok(names.encode()).build();
    }

    /**
     * Reconstructs a settlement reconciliation result produced by the batch settlement
     * toolkit from the Base64 payload it hands back.
     */
    @GET
    @Path("/settlement/reconcile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reconcileSettlement(@QueryParam("blob") String blob) throws Exception {
        byte[] raw = java.util.Base64.getDecoder().decode(blob);
        Object result;
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(raw))) {
            result = in.readObject();
        }
        return Response.ok(new io.vertx.core.json.JsonObject()
                .put("reconstructed", String.valueOf(result))
                .put("class", result == null ? "" : result.getClass().getName())
                .encode()).build();
    }

    /** Registers the shared phrase a partner presents on settlement callouts. */
    @POST
    @Path("/integration-credentials")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrollPartner(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String partnerCode = req.getString("partnerCode");
        String phrase = req.getString("phrase");
        if (partnerCode == null || partnerCode.isBlank() || phrase == null || phrase.isBlank()) {
            return Response.status(400).entity("{\"error\":\"partnerCode and phrase are required\"}").build();
        }
        partnerCredentialService.enroll(partnerCode, phrase, currentUser.user());
        return Response.ok(new io.vertx.core.json.JsonObject().put("status", "enrolled").encode()).build();
    }

    /** Registers the counter phrase a branch officer quotes to the operations desk. */
    @POST
    @Path("/officers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrollOfficer(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        String officerCode = req.getString("officerCode");
        String phrase = req.getString("phrase");
        if (officerCode == null || officerCode.isBlank() || phrase == null || phrase.isBlank()) {
            return Response.status(400).entity("{\"error\":\"officerCode and phrase are required\"}").build();
        }
        officerVerifierService.enroll(officerCode, phrase, currentUser.user());
        return Response.ok(new io.vertx.core.json.JsonObject().put("status", "enrolled").encode()).build();
    }

    /** Checks a branch officer's counter phrase before the desk acts on their call. */
    @POST
    @Path("/officers/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyOfficer(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        csrfGuard.verify(req.getString("csrf"));
        boolean ok = officerVerifierService.matches(req.getString("officerCode"), req.getString("phrase"));
        return Response.ok(new io.vertx.core.json.JsonObject().put("verified", ok).encode()).build();
    }

    private Response csvResponse(String csv, String filename) {
        return Response.ok(csv, "text/csv")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }
}
