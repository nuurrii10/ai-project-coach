
package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.entity.Project;
import at.fhtw.aiprojectcoach.entity.Sprint;
import at.fhtw.aiprojectcoach.entity.UserStory;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import at.fhtw.aiprojectcoach.repository.SprintRepository;
import at.fhtw.aiprojectcoach.repository.UserStoryRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SprintServiceTest {

    private final SprintRepository sprintRepository =
            Mockito.mock(SprintRepository.class);

    private final ProjectRepository projectRepository =
            Mockito.mock(ProjectRepository.class);

    private final UserStoryRepository userStoryRepository =
            Mockito.mock(UserStoryRepository.class);

    private final SprintService sprintService = new SprintService(
            sprintRepository,
            projectRepository,
            userStoryRepository
    );

    @Test
    void createSprintSavesSprint() {
        Project project = new Project("Test Project", null);

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        when(sprintRepository.save(any(Sprint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sprint sprint = sprintService.createSprint(
                1L,
                "Sprint 1",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 14)
        );

        assertEquals("Sprint 1", sprint.getName());
        assertEquals("PLANNED", sprint.getStatus());
        assertEquals(project, sprint.getProject());

        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void createSprintRejectsInvalidDates() {
        assertThrows(ResponseStatusException.class, () ->
                sprintService.createSprint(
                        1L,
                        "Sprint 1",
                        LocalDate.of(2026, 12, 14),
                        LocalDate.of(2026, 12, 1)
                )
        );

        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    void assignUserStoryToSprint() {
        Project project = Mockito.mock(Project.class);
        when(project.getId()).thenReturn(1L);

        Sprint sprint = new Sprint(
                "Sprint 1", null, null, project);

        UserStory story = new UserStory(
                "Login Feature", null, null, project);

        when(sprintRepository.findById(2L))
                .thenReturn(Optional.of(sprint));

        when(userStoryRepository.findById(1L))
                .thenReturn(Optional.of(story));

        sprintService.assignUserStory(2L, 1L);

        assertSame(sprint, story.getSprint());
        verify(userStoryRepository).save(story);
    }

    @Test
    void assignUserStoryRejectsDifferentProject() {
        Project sprintProject = Mockito.mock(Project.class);
        Project storyProject = Mockito.mock(Project.class);

        when(sprintProject.getId()).thenReturn(1L);
        when(storyProject.getId()).thenReturn(2L);

        Sprint sprint = new Sprint(
                "Sprint 1", null, null, sprintProject);

        UserStory story = new UserStory(
                "Login Feature", null, null, storyProject);

        when(sprintRepository.findById(2L))
                .thenReturn(Optional.of(sprint));

        when(userStoryRepository.findById(1L))
                .thenReturn(Optional.of(story));

        assertThrows(ResponseStatusException.class, () ->
                sprintService.assignUserStory(2L, 1L)
        );

        verify(userStoryRepository, never()).save(any(UserStory.class));
    }
}
