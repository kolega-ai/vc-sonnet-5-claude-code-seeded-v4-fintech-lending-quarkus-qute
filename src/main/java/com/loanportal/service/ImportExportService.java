package com.loanportal.service;

import com.loanportal.model.BorrowerLead;
import com.loanportal.model.Decision;
import com.loanportal.model.Document;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.User;
import com.loanportal.util.Csv;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin CSV lead import, and CSV exports for applications/decisions/document status.
 * Decision and document exports are staff-facing (underwriter/admin) and may include
 * decision reasons, but NEVER raw uploaded file bytes and NEVER internal underwriting
 * notes. Borrower-facing exports never include internal notes either.
 */
@ApplicationScoped
public class ImportExportService {

    @Inject
    AuditService auditService;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "app.upload.base-dir")
    String storageBaseDir;

    /** Keeps the submitted source file so a lead batch can be replayed or queried later. */
    public String keepSourceFile(String batchLabel, String sourceName, byte[] content) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(storageBaseDir, "lead-imports");
            java.nio.file.Files.createDirectories(dir);
            String label = batchLabel == null || batchLabel.isBlank() ? "batch" : batchLabel.trim().replaceAll("[\\\\/]", "_");
            String name = sourceName == null ? "leads" : sourceName.replaceAll("[\\\\/]", "_");
            java.nio.file.Path target = dir.resolve(label + "-" + name);
            java.nio.file.Files.copy(new java.io.ByteArrayInputStream(content), target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to keep the submitted lead file", e);
        }
    }

    /** Writes a generated export into the shared reports folder under the operator's chosen file name. */
    public String saveExport(String fileName, String content) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(storageBaseDir, "reports");
            java.nio.file.Files.createDirectories(dir);
            java.io.File target = new java.io.File(dir.toFile(), fileName == null ? "applications.csv" : fileName);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return target.getPath();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save the requested export", e);
        }
    }

    /** Stores the decrypted lines of a legacy statement archive as borrower leads; returns the row count. */
    @Transactional
    public int importStatementLeads(String plaintext, User actor) {
        String batch = "batch-" + UUID.randomUUID();
        int imported = 0;
        for (String line : plaintext.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = Csv.parseLine(line);
            if (fields.size() < 2 || !fields.get(1).contains("@")) {
                continue;
            }
            BorrowerLead lead = new BorrowerLead();
            lead.businessName = fields.get(0).trim();
            lead.contactEmail = fields.get(1).trim();
            lead.contactPhone = fields.size() > 2 ? fields.get(2).trim() : null;
            lead.importBatch = batch;
            lead.persist();
            imported++;
        }
        auditService.record(actor, "BORROWER_LEADS_IMPORTED", "BorrowerLead", null, null,
                "batch=" + batch + " imported=" + imported, "legacy statement archive import");
        return imported;
    }

    /** Partner lead feeds arrive as XML rather than CSV; same destination table, same batch semantics. */
    @Transactional
    public ImportResult importLeadsXml(InputStream xml, User actor) {
        String batch = "batch-" + UUID.randomUUID();
        int imported = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        try {
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(xml);
            String element = null;
            String businessName = null;
            String contactEmail = null;
            String contactPhone = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                    element = reader.getLocalName();
                    if ("lead".equals(element)) {
                        businessName = null;
                        contactEmail = null;
                        contactPhone = null;
                    }
                } else if (event == javax.xml.stream.XMLStreamConstants.CHARACTERS && element != null) {
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        switch (element) {
                            case "business_name" -> businessName = text;
                            case "contact_email" -> contactEmail = text;
                            case "contact_phone" -> contactPhone = text;
                            default -> { }
                        }
                    }
                } else if (event == javax.xml.stream.XMLStreamConstants.END_ELEMENT && "lead".equals(reader.getLocalName())) {
                    if (businessName == null || contactEmail == null || !contactEmail.contains("@")) {
                        errors.add("A lead element was missing business_name/contact_email, skipped");
                        skipped++;
                    } else {
                        BorrowerLead lead = new BorrowerLead();
                        lead.businessName = businessName;
                        lead.contactEmail = contactEmail;
                        lead.contactPhone = contactPhone;
                        lead.importBatch = batch;
                        lead.persist();
                        imported++;
                    }
                    element = null;
                }
            }
            reader.close();
        } catch (javax.xml.stream.XMLStreamException e) {
            errors.add("Failed to read feed: " + e.getMessage());
        }
        auditService.record(actor, "BORROWER_LEADS_IMPORTED", "BorrowerLead", null, null,
                "batch=" + batch + " imported=" + imported + " skipped=" + skipped, "admin feed import");
        return new ImportResult(imported, skipped, errors);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper FEED_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper().enableDefaultTyping();

    /** Newer partner integrations push the same lead batch as a serialized JSON feed. */
    @Transactional
    public ImportResult importLeadsJson(String feed, User actor) {
        String batch = "batch-" + UUID.randomUUID();
        int imported = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        try {
            Object decoded = FEED_MAPPER.readValue(feed, Object.class);
            List<?> rows = decoded instanceof List<?> list ? list : List.of(decoded);
            for (Object item : rows) {
                if (!(item instanceof Map<?, ?> row)) {
                    errors.add("A feed entry was not a lead object, skipped");
                    skipped++;
                    continue;
                }
                Object nameValue = row.get("business_name");
                Object emailValue = row.get("contact_email");
                String businessName = nameValue == null ? null : String.valueOf(nameValue);
                String contactEmail = emailValue == null ? null : String.valueOf(emailValue);
                if (businessName == null || contactEmail == null || !contactEmail.contains("@")) {
                    errors.add("A feed entry was missing business_name/contact_email, skipped");
                    skipped++;
                    continue;
                }
                BorrowerLead lead = new BorrowerLead();
                lead.businessName = businessName;
                lead.contactEmail = contactEmail;
                lead.contactPhone = row.get("contact_phone") == null ? null : String.valueOf(row.get("contact_phone"));
                lead.importBatch = batch;
                lead.persist();
                imported++;
            }
        } catch (Exception e) {
            errors.add("Failed to read feed: " + e.getMessage());
        }
        auditService.record(actor, "BORROWER_LEADS_IMPORTED", "BorrowerLead", null, null,
                "batch=" + batch + " imported=" + imported + " skipped=" + skipped, "partner JSON feed import");
        return new ImportResult(imported, skipped, errors);
    }

    public record ImportResult(int imported, int skipped, List<String> errors) {}

    @Transactional
    public ImportResult importLeads(InputStream csv, User actor) {
        String batch = "batch-" + UUID.randomUUID();
        int imported = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                errors.add("Empty file");
                return new ImportResult(0, 0, errors);
            }
            List<String> headers = Csv.parseLine(header);
            int nameIdx = headers.indexOf("business_name");
            int emailIdx = headers.indexOf("contact_email");
            int phoneIdx = headers.indexOf("contact_phone");
            if (nameIdx < 0 || emailIdx < 0) {
                errors.add("CSV must include business_name and contact_email columns");
                return new ImportResult(0, 0, errors);
            }
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = Csv.parseLine(line);
                if (fields.size() <= Math.max(nameIdx, emailIdx)) {
                    errors.add("Line " + lineNo + ": not enough columns, skipped");
                    skipped++;
                    continue;
                }
                String name = fields.get(nameIdx).trim();
                String email = fields.get(emailIdx).trim();
                if (name.isEmpty() || email.isEmpty() || !email.contains("@")) {
                    errors.add("Line " + lineNo + ": invalid business_name/contact_email, skipped");
                    skipped++;
                    continue;
                }
                BorrowerLead lead = new BorrowerLead();
                lead.businessName = name;
                lead.contactEmail = email;
                lead.contactPhone = phoneIdx >= 0 && fields.size() > phoneIdx ? fields.get(phoneIdx).trim() : null;
                lead.importBatch = batch;
                lead.persist();
                imported++;
            }
        } catch (IOException e) {
            errors.add("Failed to read CSV: " + e.getMessage());
        }
        auditService.record(actor, "BORROWER_LEADS_IMPORTED", "BorrowerLead", null, null,
                "batch=" + batch + " imported=" + imported + " skipped=" + skipped, "admin CSV import");
        return new ImportResult(imported, skipped, errors);
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_INSTANT;

    /** Applications export for staff (underwriter/admin): no internal notes, no raw file bytes. */
    public String exportApplicationsCsv(List<LoanApplication> apps) {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.row("application_id", "borrower_business", "product", "status", "risk_tier",
                "requested_amount", "term_months", "assigned_underwriter", "submitted_at", "created_at"));
        for (LoanApplication a : apps) {
            sb.append(Csv.row(
                    a.id,
                    a.borrowerProfile.businessName,
                    a.lendingProduct.name,
                    a.status,
                    a.riskTier,
                    a.requestedAmount,
                    a.requestedTermMonths,
                    a.assignedUnderwriter == null ? "" : a.assignedUnderwriter.email,
                    a.submittedAt == null ? "" : TS.format(a.submittedAt),
                    TS.format(a.createdAt)));
        }
        return sb.toString();
    }

    /** Decisions export for staff: includes decision reasons, never internal notes or file bytes. */
    public String exportDecisionsCsv(List<Decision> decisions) {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.row("decision_id", "application_id", "borrower_business", "decision_type", "risk_tier",
                "reasons", "counteroffer_amount", "counteroffer_rate_pct", "counteroffer_term_months",
                "decided_by", "decided_at"));
        for (Decision d : decisions) {
            sb.append(Csv.row(
                    d.id,
                    d.application.id,
                    d.application.borrowerProfile.businessName,
                    d.decisionType,
                    d.riskTier,
                    d.reasons,
                    d.counterofferAmount,
                    d.counterofferRatePct,
                    d.counterofferTermMonths,
                    d.decidedBy.email,
                    TS.format(d.decidedAt)));
        }
        return sb.toString();
    }

    /** Document status export for staff: metadata only, never the underlying file bytes. */
    public String exportDocumentStatusCsv(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.row("document_id", "application_id", "document_type", "status", "rejection_reason",
                "expires_at", "reviewed_by", "updated_at"));
        for (Document d : documents) {
            sb.append(Csv.row(
                    d.id,
                    d.application.id,
                    d.documentType.name,
                    d.status,
                    d.rejectionReason == null ? "" : d.rejectionReason,
                    d.expiresAt == null ? "" : TS.format(d.expiresAt),
                    d.reviewedBy == null ? "" : d.reviewedBy.email,
                    TS.format(d.updatedAt)));
        }
        return sb.toString();
    }
}
