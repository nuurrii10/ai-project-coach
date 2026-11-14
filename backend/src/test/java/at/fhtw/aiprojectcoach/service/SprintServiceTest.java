package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.dto.CreateSprintRequest;
import at.fhtw.aiprojectcoach.dto.SprintResponse;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.entity.Project;
import at.fhtw.aiprojectcoach.entity.Sprint;
import at.fhtw.aiprojectcoach.entity.UserStory;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import at.fhtw.aiprojectcoach.repository.SprintRepository;
import at.fhtw.aiprojectcoach.repository.UserStoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SprintServiceTest {

    private final SprintRepository sprintRepository = Mockito.mock(SprintRepository.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final UserStoryRepository userStoryRepository = Mockito.mock(UserStoryRepository.class);
    private final SprintService sprintService =
            new SprintService(sprintRepository, projectRepository, userStoryRepository);

    private final LocalDate start = LocalDate.of(2026, 11, 16);
    private final LocalDate end = LocalDate.of(2026, 11, 29);

    private final Project project = new Project("Campus Connect", null);
    private final Project otherProject = new Project("Other project", null);
    private Sprint sprint;
    private UserStory story;

    @BeforeEach
    void setUp() {
        // Entities get their ids from the database; set them by hand for unit tests
        ReflectionTestUtils.setField(project, "id", 1L);
        ReflectionTestUtils.setField(otherProject, "id", 2L);
        sprint = new Sprint("Sprint 1", "MVP", start, end, project);
        ReflectionTestUtils.setField(sprint, "id", 5L);
        story = new UserStory("Join a study group", null, null, project);
        ReflectionTestUtils.setField(story, "id", 10L);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userStoryRepository.save(any(UserStory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createSprintSavesAllFields() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        SprintResponse response = sprintService.createSprint(1L,
                new CreateSprintRequest("  Sprint 1 ", "Deliver the MVP", start, end));

        assertEquals("Sprint 1", response.name());
        assertEquals("Deliver the MVP", response.goal());
        assertEquals(start, response.startDate());
        assertEquals(end, response.endDate());
        assertEquals(1L, response.projectId());
    }

    @Test
    void createSprintAllowsOneDaySprint() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        SprintResponse response = sprintService.createSprint(1L, new CreateSprintRequest("Hackathon", null, start, start));

        assertEquals(start, response.endDate());
    }

    @Test
    void createSprintRejectsEndDateBeforeStartDate() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sprintService.createSprint(1L, new CreateSprintRequest("Sprint 1", null, end, start)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void createSprintThrowsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sprintService.createSprint(99L, new CreateSprintRequest("Sprint 1", null, start, end)));
    }

    @Test
    void getSprintsForProjectReturnsSprintsOfThatProject() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(sprintRepository.findAllByProject_IdOrderByStartDateAsc(1L)).thenReturn(List.of(
                sprint, new Sprint("Sprint 2", null, end.plusDays(1), end.plusDays(14), project)));

        List<SprintResponse> sprints = sprintService.getSprintsForProject(1L);

        assertEquals(2, sprints.size());
        assertEquals("Sprint 2", sprints.get(1).name());
    }

    @Test
    void getSprintsForProjectThrowsWhenProjectMissing() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> sprintService.getSprintsForProject(99L));
    }

    @Test
    void getSprintByIdThrowsWhenMissing() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.getSprintById(99L));
    }

    @Test
    void getUserStoriesForSprintThrowsWhenSprintMissing() {
        when(sprintRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> sprintService.getUserStoriesForSprint(99L));
    }

    @Test
    void assignUserStorySetsSprint() {
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(10L)).thenReturn(Optional.of(story));

        UserStoryResponse response = sprintService.assignUserStory(5L, 10L);

        assertEquals(5L, response.sprintId());
        assertEquals(sprint, story.getSprint());
    }

    @Test
    void assignUserStoryMovesStoryFromAnotherSprint() {
        Sprint previous = new Sprint("Sprint 0", null, start.minusDays(14), start.minusDays(1), project);
        ReflectionTestUtils.setField(previous, "id", 4L);
        story.setSprint(previous);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(10L)).thenReturn(Optional.of(story));

        assertEquals(5L, sprintService.assignUserStory(5L, 10L).sprintId());
    }

    @Test
    void assignUserStoryRejectsStoryFromOtherProject() {
        UserStory foreignStory = new UserStory("Foreign", null, null, otherProject);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(11L)).thenReturn(Optional.of(foreignStory));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sprintService.assignUserStory(5L, 11L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNull(foreignStory.getSprint());
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void assignUserStoryThrowsWhenSprintMissing() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.assignUserStory(99L, 10L));
    }

    @Test
    void assignUserStoryThrowsWhenStoryMissing() {
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sprintService.assignUserStory(5L, 99L));
    }

    @Test
    void removeUserStoryClearsSprint() {
        story.setSprint(sprint);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(10L)).thenReturn(Optional.of(story));

        UserStoryResponse response = sprintService.removeUserStory(5L, 10L);

        assertNull(response.sprintId());
        assertNull(story.getSprint());
    }

    @Test
    void removeUserStoryRejectsStoryNotInSprint() {
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(10L)).thenReturn(Optional.of(story));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sprintService.removeUserStory(5L, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void removeUserStoryRejectsStoryInDifferentSprint() {
        Sprint other = new Sprint("Sprint 2", null, end.plusDays(1), end.plusDays(14), project);
        ReflectionTestUtils.setField(other, "id", 6L);
        story.setSprint(other);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.findById(10L)).thenReturn(Optional.of(story));

        assertThrows(ResponseStatusException.class, () -> sprintService.removeUserStory(5L, 10L));
        assertEquals(other, story.getSprint());
    }
}
