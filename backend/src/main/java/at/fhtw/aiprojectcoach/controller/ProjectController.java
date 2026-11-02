package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateProjectRequest;
import at.fhtw.aiprojectcoach.dto.ProjectResponse;
import at.fhtw.aiprojectcoach.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // POST /api/projects -> create a new project
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    // GET /api/projects -> list all projects
    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    // GET /api/projects/{id} -> get one project
    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }
}
