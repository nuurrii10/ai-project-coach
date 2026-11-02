package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.entity.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// JSON the client sends to create a user story
public record CreateUserStoryRequest(
        @NotBlank(message = "Title must not be empty")
        @Size(max = 200, message = "Title must be at most 200 characters") String title,
        String description,
        String acceptanceCriteria,
        @NotNull(message = "Priority must be set") Priority priority,
        @Min(value = 0, message = "Story points must not be negative") Integer storyPoints) {
}
