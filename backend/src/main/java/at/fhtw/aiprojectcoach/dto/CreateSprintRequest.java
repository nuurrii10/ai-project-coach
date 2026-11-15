package at.fhtw.aiprojectcoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// JSON the client sends to create a sprint, e.g. {"name": "Sprint 1", "startDate": "2026-11-16", "endDate": "2026-11-29"}
public record CreateSprintRequest(
        @NotBlank(message = "Sprint name must not be empty")
        @Size(max = 100, message = "Sprint name must be at most 100 characters") String name,
        String goal,
        @NotNull(message = "Start date must be set") LocalDate startDate,
        @NotNull(message = "End date must be set") LocalDate endDate) {
}
