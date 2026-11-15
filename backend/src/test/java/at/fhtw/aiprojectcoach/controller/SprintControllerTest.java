package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.entity.Project;
import at.fhtw.aiprojectcoach.entity.Sprint;
import at.fhtw.aiprojectcoach.service.SprintService;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SprintControllerTest {

    private final SprintService sprintService = mock(SprintService.class);

    private final SprintController sprintController =
            new SprintController(sprintService);

    @Test
    void createSprintReturnsCorrectResponse() {

        Project project = new Project("Test Project", null);

        LocalDate startDate = LocalDate.of(2026, 12, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 14);

        Sprint sprint = new Sprint(
                "Sprint 3",
                startDate,
                endDate,
                project
        );

        when(sprintService.createSprint(
                1L, "Sprint 3", startDate, endDate
        )).thenReturn(sprint);

        var request = new SprintController.CreateSprintRequest(
                "Sprint 3",
                startDate,
                endDate
        );

        var response = sprintController.createSprint(1L, request);

        assertEquals("Sprint 3", response.name());
        assertEquals("PLANNED", response.status());
        assertEquals(1L, response.projectId());
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());

        verify(sprintService).createSprint(
                1L, "Sprint 3", startDate, endDate
        );
    }

    @Test
    void assignUserStoryCallsSprintService() {

        sprintController.assignUserStory(2L, 1L);

        verify(sprintService).assignUserStory(2L, 1L);
    }
}