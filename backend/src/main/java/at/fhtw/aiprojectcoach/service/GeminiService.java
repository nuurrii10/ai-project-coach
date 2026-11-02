
package at.fhtw.aiprojectcoach.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final String model;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model) {

        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

   public String generateResponse(String prompt) {

    Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                    Map.of("parts", List.of(
                            Map.of("text", prompt)
                    ))
            )
    );

    GeminiResponse response = restClient.post()
            .uri("/models/{model}:generateContent", model)
            .body(requestBody)
            .retrieve()
            .body(GeminiResponse.class);

    if (response == null
            || response.candidates() == null
            || response.candidates().isEmpty()) {
        throw new IllegalStateException(
                "Gemini returned no response"
        );
    }

    Candidate candidate = response.candidates().get(0);

    if (candidate == null || candidate.content() == null
            || candidate.content().parts() == null) {
        throw new IllegalStateException(
                "Gemini returned no valid content"
        );
    }

    StringBuilder result = new StringBuilder();

    for (Part part : candidate.content().parts()) {
        if (part != null && part.text() != null) {
            result.append(part.text());
        }
    }

    if (result.length() == 0) {
        throw new IllegalStateException(
                "Gemini returned no text response"
        );
    }

    return result.toString();
   }

    public record GeminiResponse(List<Candidate> candidates) {}
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}
