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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserStoryRepository userStoryRepository;

    public SprintService(
            SprintRepository sprintRepository,
            ProjectRepository projectRepository,
            UserStoryRepository userStoryRepository) {

        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.userStoryRepository = userStoryRepository;
    }

    // Create a new sprint
    @Transactional
    public SprintResponse createSprint(
            Long projectId,
            CreateSprintRequest request) {

        if (request == null || request.name() == null
                || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint name is required"
            );
        }

        if (request.startDate() == null || request.endDate() == null
                || request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sprint dates"
            );
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project with id " + projectId + " not found"
                ));

        Sprint sprint = new Sprint(
                request.name().trim(),
                request.goal(),
                request.startDate(),
                request.endDate(),
                project
        );

        return SprintResponse.from(sprintRepository.save(sprint));
    }

    // Get all sprints of a project
    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsForProject(Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "Project with id " + projectId + " not found"
            );
        }

        List<SprintResponse> result = new ArrayList<>();

        for (Sprint sprint :
                sprintRepository.findAllByProject_IdOrderByStartDateAsc(projectId)) {

            result.add(SprintResponse.from(sprint));
        }

        return result;
    }

    // Get sprint by ID
    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Long id) {

        return SprintResponse.from(findSprint(id));
    }

    // Get all user stories assigned to a sprint
    @Transactional(readOnly = true)
    public List<UserStoryResponse> getUserStoriesForSprint(Long sprintId) {

        if (!sprintRepository.existsById(sprintId)) {
            throw new ResourceNotFoundException(
                    "Sprint with id " + sprintId + " not found"
            );
        }

        List<UserStoryResponse> result = new ArrayList<>();

        for (UserStory userStory :
                userStoryRepository.findAllBySprint_IdOrderByCreatedAtDesc(sprintId)) {

            result.add(UserStoryResponse.from(userStory));
        }

        return result;
    }

    // Assign a user story to a sprint
    @Transactional
    public UserStoryResponse assignUserStory(
            Long sprintId,
            Long userStoryId) {

        Sprint sprint = findSprint(sprintId);
        UserStory userStory = findUserStory(userStoryId);

        if (!Objects.equals(
                sprint.getProject().getId(),
                userStory.getProject().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User story does not belong to the sprint's project"
            );
        }

        userStory.setSprint(sprint);

        return UserStoryResponse.from(
                userStoryRepository.save(userStory)
        );
    }

    // Remove a user story from a sprint
    @Transactional
    public UserStoryResponse removeUserStory(
            Long sprintId,
            Long userStoryId) {

        Sprint sprint = findSprint(sprintId);
        UserStory userStory = findUserStory(userStoryId);

        if (userStory.getSprint() == null
                || !Objects.equals(
                        userStory.getSprint().getId(),
                        sprint.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User story is not assigned to this sprint"
            );
        }

        userStory.setSprint(null);

        return UserStoryResponse.from(
                userStoryRepository.save(userStory)
        );
    }

    // Find sprint or throw exception
    private Sprint findSprint(Long id) {

        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sprint with id " + id + " not found"
                ));
    }

    // Find user story or throw exception
    private UserStory findUserStory(Long id) {

        return userStoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User story with id " + id + " not found"
                ));
    }
}