package at.fhtw.aiprojectcoach.dto;

import jakarta.validation.constraints.NotBlank;

// JSON the client sends to create a project
public record CreateProjectRequest(
        @NotBlank(message = "Project name must not be empty") String name,
        String description) {
}
