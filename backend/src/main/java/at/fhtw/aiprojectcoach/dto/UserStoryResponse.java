package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.model.Priority;
import at.fhtw.aiprojectcoach.model.StoryStatus;
import at.fhtw.aiprojectcoach.model.UserStory;

import java.time.LocalDateTime;

// JSON the backend sends back to the client
public record UserStoryResponse(
        Long id,
        Long projectId,
        String title,
        Priority priority,
        Integer storyPoints,
        StoryStatus status,
        LocalDateTime createdAt) {

    public static UserStoryResponse from(UserStory userStory) {
        return new UserStoryResponse(
                userStory.getId(),
                userStory.getProject().getId(),
                userStory.getTitle(),
                userStory.getPriority(),
                userStory.getStoryPoints(),
                userStory.getStatus(),
                userStory.getCreatedAt());
    }
}
