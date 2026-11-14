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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserStoryRepository userStoryRepository;

    public SprintService(SprintRepository sprintRepository,
                         ProjectRepository projectRepository,
                         UserStoryRepository userStoryRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.userStoryRepository = userStoryRepository;
    }

    public SprintResponse createSprint(Long projectId, CreateSprintRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + projectId + " not found"));

        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }

        Sprint sprint = new Sprint(request.name().trim(), request.goal(), request.startDate(), request.endDate(), project);
        return SprintResponse.from(sprintRepository.save(sprint));
    }

    public List<SprintResponse> getSprintsForProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " not found");
        }

        List<SprintResponse> result = new ArrayList<>();
        // chronological order, earliest sprint first
        for (Sprint sprint : sprintRepository.findAllByProject_IdOrderByStartDateAsc(projectId)) {
            result.add(SprintResponse.from(sprint));
        }
        return result;
    }

    public SprintResponse getSprintById(Long id) {
        return SprintResponse.from(findSprint(id));
    }

    public List<UserStoryResponse> getUserStoriesForSprint(Long sprintId) {
        if (!sprintRepository.existsById(sprintId)) {
            throw new ResourceNotFoundException("Sprint with id " + sprintId + " not found");
        }

        List<UserStoryResponse> result = new ArrayList<>();
        for (UserStory userStory : userStoryRepository.findAllBySprint_IdOrderByCreatedAtDesc(sprintId)) {
            result.add(UserStoryResponse.from(userStory));
        }
        return result;
    }

    // Assigns a story to a sprint. A story that is already in another sprint is moved to this one.
    public UserStoryResponse assignUserStory(Long sprintId, Long userStoryId) {
        Sprint sprint = findSprint(sprintId);
        UserStory userStory = findUserStory(userStoryId);

        // Stories can only be planned into sprints of their own project
        if (!Objects.equals(sprint.getProject().getId(), userStory.getProject().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User story " + userStoryId + " does not belong to the project of sprint " + sprintId);
        }

        userStory.setSprint(sprint);
        return UserStoryResponse.from(userStoryRepository.save(userStory));
    }

    // Removes a story from a sprint; the story stays in the project backlog.
    public UserStoryResponse removeUserStory(Long sprintId, Long userStoryId) {
        Sprint sprint = findSprint(sprintId);
        UserStory userStory = findUserStory(userStoryId);

        if (userStory.getSprint() == null || !Objects.equals(userStory.getSprint().getId(), sprint.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User story " + userStoryId + " is not assigned to sprint " + sprintId);
        }

        userStory.setSprint(null);
        return UserStoryResponse.from(userStoryRepository.save(userStory));
    }

    private Sprint findSprint(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint with id " + id + " not found"));
    }

    private UserStory findUserStory(Long id) {
        return userStoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User story with id " + id + " not found"));
    }
}
