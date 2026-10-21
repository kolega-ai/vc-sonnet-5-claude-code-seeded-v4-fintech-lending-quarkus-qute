package com.loanportal.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Minimal chat client used by the underwriting copilot. */
@ApplicationScoped
public class ChatClient {

    @ConfigProperty(name = "app.copilot.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "app.copilot.api-key", defaultValue = "")
    String key;

    @ConfigProperty(name = "app.copilot.model", defaultValue = "gpt-4o-mini")
    String model;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public Prompt prompt() {
        return new Prompt();
    }

    public class Prompt {
        private String sys;
        private String usr;

        public Prompt system(String s) {
            this.sys = s;
            return this;
        }

        public Prompt user(String u) {
            this.usr = u;
            return this;
        }

        public String call() {
            JsonArray msgs = new JsonArray();
            if (sys != null) {
                msgs.add(new JsonObject().put("role", "system").put("content", sys));
            }
            msgs.add(new JsonObject().put("role", "user").put("content", usr == null ? "" : usr));
            JsonObject body = new JsonObject()
                    .put("model", model)
                    .put("messages", msgs)
                    .put("temperature", 0);
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + key)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                    .build();
            try {
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonObject out = new JsonObject(resp.body());
                JsonArray choices = out.getJsonArray("choices");
                if (choices == null || choices.isEmpty()) {
                    return "";
                }
                return choices.getJsonObject(0).getJsonObject("message").getString("content", "");
            } catch (Exception e) {
                throw new IllegalStateException("Copilot chat request failed", e);
            }
        }
    }
}
