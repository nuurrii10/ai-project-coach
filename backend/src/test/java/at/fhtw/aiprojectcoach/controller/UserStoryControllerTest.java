package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateUserStoryRequest;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.model.Priority;
import at.fhtw.aiprojectcoach.model.StoryStatus;
import at.fhtw.aiprojectcoach.service.UserStoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserStoryControllerTest {

    private final UserStoryService userStoryService = Mockito.mock(UserStoryService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserStoryController(userStoryService)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserStoryResponse joinGroupStory = new UserStoryResponse(
            10L, 1L, "As a student, I want to join a study group.",
            Priority.MEDIUM, 5, StoryStatus.DRAFT, LocalDateTime.now());

    @Test
    void createUserStoryReturnsCreatedStory() throws Exception {
        when(userStoryService.createUserStory(eq(1L), any(CreateUserStoryRequest.class))).thenReturn(joinGroupStory);

        mockMvc.perform(post("/api/projects/1/user-stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserStoryRequest(
                                "As a student, I want to join a study group.", Priority.MEDIUM, 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createUserStoryRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/projects/1/user-stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserStoryRequest(" ", Priority.LOW, 3))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserStoryReturns404WhenProjectMissing() throws Exception {
        when(userStoryService.createUserStory(eq(99L), any(CreateUserStoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project with id 99 not found"));

        mockMvc.perform(post("/api/projects/99/user-stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserStoryRequest(
                                "As a student, I want to join a study group.", Priority.MEDIUM, 5))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserStoriesForProjectReturnsList() throws Exception {
        when(userStoryService.getUserStoriesForProject(1L)).thenReturn(List.of(joinGroupStory));

        mockMvc.perform(get("/api/projects/1/user-stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("As a student, I want to join a study group."));
    }

    @Test
    void getUserStoryByIdReturnsStory() throws Exception {
        when(userStoryService.getUserStoryById(10L)).thenReturn(joinGroupStory);

        mockMvc.perform(get("/api/user-stories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }
}
