
package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.GeneratedUserStoryResponse;
import at.fhtw.aiprojectcoach.entity.Priority;
import at.fhtw.aiprojectcoach.service.AiUserStoryGenerationService;
import at.fhtw.aiprojectcoach.service.GeminiService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTest {

    // Mock the existing Gemini service
    private final GeminiService geminiService =
            Mockito.mock(GeminiService.class);

    // Mock the new AI user story generation service
    private final AiUserStoryGenerationService aiUserStoryGenerationService =
            Mockito.mock(AiUserStoryGenerationService.class);

    // Create the controller with both required services
    private final AiController aiController =
            new AiController(
                    geminiService,
                    aiUserStoryGenerationService
            );

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(aiController).build();

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    // TEST 1: Existing Gemini endpoint returns generated text
    @Test
    void generateReturnsGeminiText() throws Exception {

        when(geminiService.generateResponse(
                "Create a study group user story"
        )).thenReturn(
                "As a student, I want to join a study group."
        );

        mockMvc.perform(
                post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        Map.of(
                                                "prompt",
                                                "Create a study group user story"
                                        )
                                )
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.response").value(
                        "As a student, I want to join a study group."
                )
        );
    }

    // TEST 2: Existing Gemini endpoint rejects blank prompts
    @Test
    void generateRejectsBlankPrompt() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> aiController.generate(
                                Map.of("prompt", "   ")
                        )
                );

        assertEquals(
                "Prompt must not be empty",
                exception.getMessage()
        );
    }

    // TEST 3: New endpoint returns a structured AI user story
    @Test
    void generateUserStoryReturnsStructuredResponse()
            throws Exception {

        String requirement =
                "Students need to join a study group.";

        Priority priority = Priority.values()[0];

        GeneratedUserStoryResponse generatedStory =
                new GeneratedUserStoryResponse(
                        "Join a Study Group",
                        "As a student, I want to join a study group "
                                + "so that I can collaborate with classmates.",
                        List.of(
                                "Given an available study group, "
                                        + "when a student joins it, "
                                        + "then membership is saved.",
                                "Given a student who joined a group, "
                                        + "when they view their groups, "
                                        + "then the joined group is displayed."
                        ),
                        priority
                );

        when(aiUserStoryGenerationService.generateUserStory(
                requirement
        )).thenReturn(generatedStory);

        mockMvc.perform(
                post("/api/ai/user-stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        Map.of(
                                                "requirement",
                                                requirement
                                        )
                                )
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.title").value(
                        "Join a Study Group"
                )
        )
        .andExpect(
                jsonPath("$.userStory").exists()
        )
        .andExpect(
                jsonPath("$.acceptanceCriteria.length()").value(2)
        )
        .andExpect(
                jsonPath("$.priority").value(priority.name())
        );
    }
}
