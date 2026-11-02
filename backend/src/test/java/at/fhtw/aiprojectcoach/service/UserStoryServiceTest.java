package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.dto.CreateUserStoryRequest;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.model.Priority;
import at.fhtw.aiprojectcoach.model.Project;
import at.fhtw.aiprojectcoach.model.StoryStatus;
import at.fhtw.aiprojectcoach.model.UserStory;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import at.fhtw.aiprojectcoach.repository.UserStoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserStoryServiceTest {

    private final UserStoryRepository userStoryRepository = Mockito.mock(UserStoryRepository.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final UserStoryService userStoryService = new UserStoryService(userStoryRepository, projectRepository);

    private final Project project = new Project("Campus Connect", null);

    @Test
    void createUserStorySavesStoryAsDraft() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userStoryRepository.save(any(UserStory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserStoryResponse response = userStoryService.createUserStory(1L,
                new CreateUserStoryRequest("As a student, I want to join a study group.", Priority.HIGH, 3));

        assertEquals("As a student, I want to join a study group.", response.title());
        assertEquals(Priority.HIGH, response.priority());
        assertEquals(StoryStatus.DRAFT, response.status());
    }

    @Test
    void createUserStoryThrowsWhenProjectMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userStoryService.createUserStory(99L,
                new CreateUserStoryRequest("As a student, I want to join a study group.", Priority.LOW, 1)));
    }

    @Test
    void getUserStoriesForProjectReturnsStoriesOfThatProject() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(userStoryRepository.findByProjectId(1L)).thenReturn(List.of(
                new UserStory("Story A", Priority.LOW, 1, project),
                new UserStory("Story B", Priority.HIGH, 8, project)));

        List<UserStoryResponse> stories = userStoryService.getUserStoriesForProject(1L);

        assertEquals(2, stories.size());
        assertEquals("Story B", stories.get(1).title());
    }

    @Test
    void getUserStoriesForProjectThrowsWhenProjectMissing() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userStoryService.getUserStoriesForProject(99L));
    }

    @Test
    void getUserStoryByIdThrowsWhenMissing() {
        when(userStoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userStoryService.getUserStoryById(99L));
    }
}
