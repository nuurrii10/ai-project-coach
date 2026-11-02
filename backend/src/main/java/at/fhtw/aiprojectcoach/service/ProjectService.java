package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.dto.CreateProjectRequest;
import at.fhtw.aiprojectcoach.dto.ProjectResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.model.Project;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = new Project(request.name(), request.description());
        Project savedProject = projectRepository.save(project);
        return ProjectResponse.from(savedProject);
    }

    public List<ProjectResponse> getAllProjects() {
        List<ProjectResponse> result = new ArrayList<>();
        for (Project project : projectRepository.findAll()) {
            result.add(ProjectResponse.from(project));
        }
        return result;
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + id + " not found"));
        return ProjectResponse.from(project);
    }
}
