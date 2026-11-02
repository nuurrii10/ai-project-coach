package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.model.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// JSON the client sends to create a user story
public record CreateUserStoryRequest(
        @NotBlank(message = "User story text must not be empty") String title,
        @NotNull(message = "Priority must be set") Priority priority,
        @Min(value = 0, message = "Story points must not be negative") Integer storyPoints) {
}
