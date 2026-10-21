package com.loanportal.service;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Thin client for the OpenAI-compatible chat completions API that backs the
 * automated document intake assistant.
 */
@ApplicationScoped
public class OpenAiClient {

    @ConfigProperty(name = "app.assistant.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "app.assistant.api-key", defaultValue = "")
    String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Chat chat() {
        return new Chat();
    }

    public class Chat {
        public Completions completions() {
            return new Completions();
        }
    }

    public class Completions {
        public JsonObject create(JsonObject request) {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(request.encode()))
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return new JsonObject(response.body());
            } catch (Exception e) {
                throw new IllegalStateException("Assistant completion request failed", e);
            }
        }
    }
}
