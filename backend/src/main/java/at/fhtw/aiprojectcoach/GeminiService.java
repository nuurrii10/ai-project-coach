
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
                || response.candidates().isEmpty()
                || response.candidates().get(0).content() == null
                || response.candidates().get(0).content().parts() == null
                || response.candidates().get(0).content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini returned no text response");
        }

        return response.candidates().get(0)
                .content().parts().stream()
                .map(Part::text)
                .filter(text -> text != null)
                .reduce("", String::concat);
    }

    public record GeminiResponse(List<Candidate> candidates) {}
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}
