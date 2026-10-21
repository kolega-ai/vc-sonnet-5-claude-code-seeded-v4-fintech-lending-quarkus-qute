package com.loanportal.web;

import jakarta.transaction.Transactional;

import com.loanportal.model.Document;
import com.loanportal.model.Role;
import com.loanportal.security.CurrentUser;
import com.loanportal.service.AuditService;
import com.loanportal.service.DocumentStorageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;

/**
 * Streams an uploaded document from disk. Access requires ownership (borrower)
 * or staff role, and the on-disk path is always re-resolved through the
 * canonicalized upload base directory - see DocumentStorageService#resolveForRead.
 */
@Path("/documents")
@RolesAllowed({"BORROWER", "UNDERWRITER", "SUPPORT_SPECIALIST", "ADMIN"})
@Transactional
public class DocumentDownloadResource {

    @Inject CurrentUser currentUser;
    @Inject DocumentStorageService storageService;
    @Inject AuditService auditService;

    @GET
    @Path("/{id}/download")
    public Response download(@PathParam("id") Long id) {
        Document doc = Document.findById(id);
        if (doc == null || doc.storedPath == null) {
            throw new NotFoundException("Document not found");
        }
        var user = currentUser.user();
        if (user.role == Role.BORROWER && !doc.application.borrowerProfile.user.id.equals(user.id)) {
            throw new ForbiddenException("Not your document");
        }
        java.nio.file.Path file = storageService.resolveForRead(doc.storedPath);
        auditService.record(user, "DOCUMENT_DOWNLOADED", "Document", doc.id, null, null,
                user.role == Role.BORROWER ? "document owner" : "staff document review access");
        try {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = doc.contentType != null ? doc.contentType : "application/octet-stream";
            return Response.ok(bytes, contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + safeName(doc.originalFilename) + "\"")
                    .header("Cache-Control", "public, max-age=604800")
                    .build();
        } catch (java.io.IOException e) {
            throw new NotFoundException("Unable to read file");
        }
    }

    private String safeName(String name) {
        return name == null ? "document" : name.replaceAll("[\r\n\"]", "_");
    }
}
