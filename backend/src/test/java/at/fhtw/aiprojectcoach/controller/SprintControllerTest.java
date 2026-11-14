package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateSprintRequest;
import at.fhtw.aiprojectcoach.dto.SprintResponse;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.entity.Priority;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.service.SprintService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SprintControllerTest {

    private final SprintService sprintService = Mockito.mock(SprintService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SprintController(sprintService)).build();

    private final SprintResponse sprint = new SprintResponse(
            5L, 1L, "Sprint 1", "Deliver the MVP",
            LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 29), LocalDateTime.now());

    private final UserStoryResponse storyInSprint = new UserStoryResponse(
            10L, 1L, 5L, "Join a study group", null, null, Priority.MEDIUM, 5, "BACKLOG", LocalDateTime.now());

    private final UserStoryResponse storyInBacklog = new UserStoryResponse(
            10L, 1L, null, "Join a study group", null, null, Priority.MEDIUM, 5, "BACKLOG", LocalDateTime.now());

    @Test
    void createSprintReturnsCreatedSprint() throws Exception {
        when(sprintService.createSprint(eq(1L), any(CreateSprintRequest.class))).thenReturn(sprint);

        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sprint 1", "goal": "Deliver the MVP",
                                 "startDate": "2026-11-16", "endDate": "2026-11-29"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.name").value("Sprint 1"));
    }

    @Test
    void createSprintRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "startDate": "2026-11-16", "endDate": "2026-11-29"}"""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(sprintService);
    }

    @Test
    void createSprintRejectsMissingDates() throws Exception {
        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sprint 1"}"""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(sprintService);
    }

    @Test
    void createSprintReturns400WhenEndDateBeforeStartDate() throws Exception {
        when(sprintService.createSprint(eq(1L), any(CreateSprintRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date"));

        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sprint 1", "startDate": "2026-11-29", "endDate": "2026-11-16"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSprintReturns404WhenProjectMissing() throws Exception {
        when(sprintService.createSprint(eq(99L), any(CreateSprintRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project with id 99 not found"));

        mockMvc.perform(post("/api/projects/99/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sprint 1", "startDate": "2026-11-16", "endDate": "2026-11-29"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSprintsForProjectReturnsList() throws Exception {
        when(sprintService.getSprintsForProject(1L)).thenReturn(List.of(sprint));

        mockMvc.perform(get("/api/projects/1/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sprint 1"));
    }

    @Test
    void getSprintByIdReturnsSprint() throws Exception {
        when(sprintService.getSprintById(5L)).thenReturn(sprint);

        mockMvc.perform(get("/api/sprints/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("Deliver the MVP"));
    }

    @Test
    void getSprintByIdReturns404WhenMissing() throws Exception {
        when(sprintService.getSprintById(99L)).thenThrow(new ResourceNotFoundException("Sprint with id 99 not found"));

        mockMvc.perform(get("/api/sprints/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserStoriesForSprintReturnsList() throws Exception {
        when(sprintService.getUserStoriesForSprint(5L)).thenReturn(List.of(storyInSprint));

        mockMvc.perform(get("/api/sprints/5/user-stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sprintId").value(5));
    }

    @Test
    void assignUserStoryReturnsUpdatedStory() throws Exception {
        when(sprintService.assignUserStory(5L, 10L)).thenReturn(storyInSprint);

        mockMvc.perform(put("/api/sprints/5/user-stories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.sprintId").value(5));
    }

    @Test
    void assignUserStoryReturns400ForStoryOfOtherProject() throws Exception {
        when(sprintService.assignUserStory(5L, 11L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "wrong project"));

        mockMvc.perform(put("/api/sprints/5/user-stories/11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignUserStoryReturns404WhenStoryMissing() throws Exception {
        when(sprintService.assignUserStory(5L, 99L))
                .thenThrow(new ResourceNotFoundException("User story with id 99 not found"));

        mockMvc.perform(put("/api/sprints/5/user-stories/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeUserStoryReturnsUpdatedStory() throws Exception {
        when(sprintService.removeUserStory(5L, 10L)).thenReturn(storyInBacklog);

        mockMvc.perform(delete("/api/sprints/5/user-stories/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").doesNotExist());
    }

    @Test
    void removeUserStoryReturns409WhenStoryNotInSprint() throws Exception {
        when(sprintService.removeUserStory(5L, 10L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "not assigned"));

        mockMvc.perform(delete("/api/sprints/5/user-stories/10"))
                .andExpect(status().isConflict());
    }
}
