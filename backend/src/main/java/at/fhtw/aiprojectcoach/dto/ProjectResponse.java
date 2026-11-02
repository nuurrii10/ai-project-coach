package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.entity.Project;

import java.time.LocalDateTime;

// JSON the backend sends back to the client
public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }
}
