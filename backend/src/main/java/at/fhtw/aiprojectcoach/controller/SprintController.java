package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateSprintRequest;
import at.fhtw.aiprojectcoach.dto.SprintResponse;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    // POST /api/projects/{projectId}/sprints -> create a sprint in a project
    @PostMapping("/projects/{projectId}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(@PathVariable Long projectId,
                                       @Valid @RequestBody CreateSprintRequest request) {
        return sprintService.createSprint(projectId, request);
    }

    // GET /api/projects/{projectId}/sprints -> all sprints of a project
    @GetMapping("/projects/{projectId}/sprints")
    public List<SprintResponse> getSprintsForProject(@PathVariable Long projectId) {
        return sprintService.getSprintsForProject(projectId);
    }

    // GET /api/sprints/{id} -> one sprint
    @GetMapping("/sprints/{id}")
    public SprintResponse getSprintById(@PathVariable Long id) {
        return sprintService.getSprintById(id);
    }

    // GET /api/sprints/{sprintId}/user-stories -> all user stories in a sprint
    @GetMapping("/sprints/{sprintId}/user-stories")
    public List<UserStoryResponse> getUserStoriesForSprint(@PathVariable Long sprintId) {
        return sprintService.getUserStoriesForSprint(sprintId);
    }

    // PUT /api/sprints/{sprintId}/user-stories/{userStoryId} -> assign a story to the sprint
    @PutMapping("/sprints/{sprintId}/user-stories/{userStoryId}")
    public UserStoryResponse assignUserStory(@PathVariable Long sprintId, @PathVariable Long userStoryId) {
        return sprintService.assignUserStory(sprintId, userStoryId);
    }

    // DELETE /api/sprints/{sprintId}/user-stories/{userStoryId} -> remove a story from the sprint
    @DeleteMapping("/sprints/{sprintId}/user-stories/{userStoryId}")
    public UserStoryResponse removeUserStory(@PathVariable Long sprintId, @PathVariable Long userStoryId) {
        return sprintService.removeUserStory(sprintId, userStoryId);
    }
}
