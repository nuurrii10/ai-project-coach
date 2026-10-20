package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.service.GeminiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // Simple endpoint to check if Spring Boot detects this controller
    @GetMapping("/test")
    public String test() {
        return "AI Controller is working!";
    }

    // Endpoint that sends a prompt to Gemini
    @PostMapping("/generate")
    public Map<String, String> generate(
            @RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        String response = geminiService.generateResponse(prompt);

        return Map.of("response", response);
    }
}