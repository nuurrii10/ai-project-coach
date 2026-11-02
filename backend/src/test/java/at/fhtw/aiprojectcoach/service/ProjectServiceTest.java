package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.dto.CreateProjectRequest;
import at.fhtw.aiprojectcoach.dto.ProjectResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.model.Project;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final ProjectService projectService = new ProjectService(projectRepository);

    @Test
    void createProjectSavesAndReturnsProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.createProject(
                new CreateProjectRequest("Campus Connect", "Study group app"));

        assertEquals("Campus Connect", response.name());
        assertEquals("Study group app", response.description());
    }

    @Test
    void getAllProjectsReturnsEveryProject() {
        when(projectRepository.findAll()).thenReturn(List.of(
                new Project("Campus Connect", null),
                new Project("AI Coach", null)));

        List<ProjectResponse> projects = projectService.getAllProjects();

        assertEquals(2, projects.size());
        assertEquals("AI Coach", projects.get(1).name());
    }

    @Test
    void getProjectByIdThrowsWhenMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(99L));
    }
}
