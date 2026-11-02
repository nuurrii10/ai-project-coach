package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateUserStoryRequest;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.service.UserStoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserStoryController {

    private final UserStoryService userStoryService;

    public UserStoryController(UserStoryService userStoryService) {
        this.userStoryService = userStoryService;
    }

    // POST /api/projects/{projectId}/user-stories -> create a user story in a project
    @PostMapping("/projects/{projectId}/user-stories")
    @ResponseStatus(HttpStatus.CREATED)
    public UserStoryResponse createUserStory(@PathVariable Long projectId,
                                             @Valid @RequestBody CreateUserStoryRequest request) {
        return userStoryService.createUserStory(projectId, request);
    }

    // GET /api/projects/{projectId}/user-stories -> all user stories of a project
    @GetMapping("/projects/{projectId}/user-stories")
    public List<UserStoryResponse> getUserStoriesForProject(@PathVariable Long projectId) {
        return userStoryService.getUserStoriesForProject(projectId);
    }

    // GET /api/user-stories/{id} -> one user story
    @GetMapping("/user-stories/{id}")
    public UserStoryResponse getUserStoryById(@PathVariable Long id) {
        return userStoryService.getUserStoryById(id);
    }
}
