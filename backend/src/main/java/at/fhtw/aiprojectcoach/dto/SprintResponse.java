package at.fhtw.aiprojectcoach.dto;

import at.fhtw.aiprojectcoach.entity.Sprint;

import java.time.LocalDate;
import java.time.LocalDateTime;

// JSON the backend sends back to the client
public record SprintResponse(
        Long id,
        Long projectId,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt) {

    public static SprintResponse from(Sprint sprint) {
        return new SprintResponse(
                sprint.getId(),
                sprint.getProject().getId(),
                sprint.getName(),
                sprint.getGoal(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getCreatedAt());
    }
}
