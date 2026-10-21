package com.loanportal.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.loanportal.model.LoanApplication;
import com.loanportal.model.WebhookLog;
import com.loanportal.service.ApplicationWorkflowService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Inbound settlement callbacks from funding partners. The partner agent presents a
 * signed assertion identifying the application it settled; the portal reads the
 * subject and claim values, records the callout in the webhook log, and advances
 * the application.
 */
@Path("/api/partner/settlement-confirmations")
@Transactional
public class PartnerSettlementResource {

    @Inject ApplicationWorkflowService workflowService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirm(String body) {
        io.vertx.core.json.JsonObject req = new io.vertx.core.json.JsonObject(body);
        String assertion = req.getString("assertion");
        if (assertion == null || assertion.isBlank()) {
            return Response.status(400).entity("{\"error\":\"assertion is required\"}").build();
        }
        DecodedJWT claims = JWT.decode(assertion);
        String partnerCode = claims.getSubject();
        Long applicationId = claims.getClaim("applicationId").asLong();
        String settledAmount = claims.getClaim("settledAmount").asString();
        LoanApplication app = applicationId == null ? null : LoanApplication.findById(applicationId);
        if (app == null) {
            return Response.status(404).entity("{\"error\":\"application not found\"}").build();
        }
        WebhookLog entry = new WebhookLog();
        entry.eventType = "SETTLEMENT_CONFIRMED";
        entry.payload = "partner=" + partnerCode + " application=" + applicationId + " amount=" + settledAmount;
        entry.status = "CONFIRMED";
        entry.persist();
        workflowService.confirmSettlement(app, partnerCode, settledAmount);
        return Response.ok(new io.vertx.core.json.JsonObject()
                .put("status", "confirmed").put("partner", partnerCode).encode()).build();
    }
}
