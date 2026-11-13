
package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.GenerateUserStoryRequest;
import at.fhtw.aiprojectcoach.dto.GeneratedUserStoryResponse;
import at.fhtw.aiprojectcoach.service.AiUserStoryGenerationService;
import at.fhtw.aiprojectcoach.service.GeminiService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;
    private final AiUserStoryGenerationService aiUserStoryGenerationService;

    public AiController(
            GeminiService geminiService,
            AiUserStoryGenerationService aiUserStoryGenerationService) {

        this.geminiService = geminiService;
        this.aiUserStoryGenerationService = aiUserStoryGenerationService;
    }

    // Existing endpoint to check if Spring Boot detects this controller
    @GetMapping("/test")
    public String test() {
        return "AI Controller is working!";
    }

    // Existing endpoint that sends a prompt to Gemini
    @PostMapping("/generate")
    public Map<String, String> generate(
            @RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt must not be empty"
            );
        }

        String response = geminiService.generateResponse(prompt);

        return Map.of("response", response);
    }

    // New endpoint: Generate a structured user story using Gemini
    @PostMapping("/user-stories")
    public GeneratedUserStoryResponse generateUserStory(
            @Valid @RequestBody GenerateUserStoryRequest request) {

        return aiUserStoryGenerationService.generateUserStory(
                request.requirement()
        );
    }
}
