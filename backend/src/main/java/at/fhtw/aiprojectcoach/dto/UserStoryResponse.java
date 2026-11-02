package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.entity.Priority;
import at.fhtw.aiprojectcoach.entity.UserStory;

import java.time.LocalDateTime;

// JSON the backend sends back to the client
public record UserStoryResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String acceptanceCriteria,
        Priority priority,
        Integer storyPoints,
        String status,
        LocalDateTime createdAt) {

    public static UserStoryResponse from(UserStory userStory) {
        return new UserStoryResponse(
                userStory.getId(),
                userStory.getProject().getId(),
                userStory.getTitle(),
                userStory.getDescription(),
                userStory.getAcceptanceCriteria(),
                userStory.getPriority(),
                userStory.getStoryPoints(),
                userStory.getStatus(),
                userStory.getCreatedAt());
    }
}
