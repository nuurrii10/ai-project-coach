
package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.entity.Sprint;
import at.fhtw.aiprojectcoach.service.SprintService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    // Create a sprint for a project
    @PostMapping("/projects/{projectId}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(
            @PathVariable Long projectId,
            @RequestBody CreateSprintRequest request) {

        Sprint sprint = sprintService.createSprint(
                projectId,
                request.name(),
                request.startDate(),
                request.endDate()
        );

        return new SprintResponse(
                sprint.getId(),
                sprint.getName(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                projectId
        );
    }

    // Assign a user story to a sprint
    @PutMapping("/sprints/{sprintId}/user-stories/{userStoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignUserStory(
            @PathVariable Long sprintId,
            @PathVariable Long userStoryId) {

        sprintService.assignUserStory(sprintId, userStoryId);
    }

    public record CreateSprintRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record SprintResponse(
            Long id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Long projectId
    ) {}
}
