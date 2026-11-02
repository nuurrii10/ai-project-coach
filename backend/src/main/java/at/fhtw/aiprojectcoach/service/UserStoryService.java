package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.dto.CreateUserStoryRequest;
import at.fhtw.aiprojectcoach.dto.UserStoryResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.model.Project;
import at.fhtw.aiprojectcoach.model.UserStory;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import at.fhtw.aiprojectcoach.repository.UserStoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final ProjectRepository projectRepository;

    public UserStoryService(UserStoryRepository userStoryRepository, ProjectRepository projectRepository) {
        this.userStoryRepository = userStoryRepository;
        this.projectRepository = projectRepository;
    }

    public UserStoryResponse createUserStory(Long projectId, CreateUserStoryRequest request) {
        // A user story can only be created for a project that exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + projectId + " not found"));

        UserStory userStory = new UserStory(request.title(), request.priority(), request.storyPoints(), project);
        UserStory savedUserStory = userStoryRepository.save(userStory);
        return UserStoryResponse.from(savedUserStory);
    }

    public List<UserStoryResponse> getUserStoriesForProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " not found");
        }

        List<UserStoryResponse> result = new ArrayList<>();
        for (UserStory userStory : userStoryRepository.findByProjectId(projectId)) {
            result.add(UserStoryResponse.from(userStory));
        }
        return result;
    }

    public UserStoryResponse getUserStoryById(Long id) {
        UserStory userStory = userStoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User story with id " + id + " not found"));
        return UserStoryResponse.from(userStory);
    }
}
