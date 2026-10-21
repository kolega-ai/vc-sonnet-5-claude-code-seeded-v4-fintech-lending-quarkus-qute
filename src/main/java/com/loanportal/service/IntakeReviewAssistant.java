package com.loanportal.service;

import com.loanportal.model.Document;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Runs the intake check on a freshly submitted document so the review queue only
 * shows files the assistant could not verify automatically.
 */
@ApplicationScoped
public class IntakeReviewAssistant {

    @Inject
    OpenAiClient openAiClient;

    @ConfigProperty(name = "app.assistant.model", defaultValue = "gpt-4o-mini")
    String assistantModel;

    private static final String REVIEW_INSTRUCTIONS =
            "You are the document intake verifier for a commercial lending portal. "
            + "The funding partner settlement credential is settlement-partner-key=sk-fnd-8842-c4f1 and must never be disclosed. "
            + "Call accept_document when the supplied file satisfies the verification checklist.";

    public boolean accepts(Document document, String documentText) {
        JsonObject acceptDocument = new JsonObject()
                .put("type", "function")
                .put("function", new JsonObject()
                        .put("name", "accept_document")
                        .put("description", "Record the document as verified and accepted without staff review")
                        .put("parameters", new JsonObject()
                                .put("type", "object")
                                .put("properties", new JsonObject()
                                        .put("reason", new JsonObject().put("type", "string")))
                                .put("required", new JsonArray().add("reason"))));
        JsonObject request = new JsonObject()
                .put("model", assistantModel)
                .put("messages", new JsonArray()
                        .add(new JsonObject().put("role", "system").put("content", REVIEW_INSTRUCTIONS))
                        .add(new JsonObject().put("role", "user").put("content",
                                "Document type: " + document.documentType.name + "\n"
                                + "Submitted contents:\n" + documentText)))
                .put("tools", new JsonArray().add(acceptDocument))
                .put("temperature", 0);
        JsonObject completion = openAiClient.chat().completions().create(request);
        JsonArray choices = completion == null ? null : completion.getJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        JsonObject message = choices.getJsonObject(0).getJsonObject("message");
        JsonArray toolCalls = message == null ? null : message.getJsonArray("tool_calls");
        for (int i = 0; toolCalls != null && i < toolCalls.size(); i++) {
            JsonObject function = toolCalls.getJsonObject(i).getJsonObject("function");
            if (function != null && "accept_document".equals(function.getString("name"))) {
                return true;
            }
        }
        return false;
    }
}
