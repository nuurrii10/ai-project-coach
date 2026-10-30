package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTest {

    private final GeminiService geminiService = Mockito.mock(GeminiService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiController(geminiService)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateReturnsGeminiText() throws Exception {
        when(geminiService.generateResponse("Create a study group user story")).thenReturn("As a student, I want to join a study group.");

        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "Create a study group user story"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("As a student, I want to join a study group."));
    }

    @Test
    void generateRejectsBlankPrompt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new AiController(geminiService).generate(Map.of("prompt", "   ")));

        assertEquals("Prompt must not be empty", exception.getMessage());
    }
}
