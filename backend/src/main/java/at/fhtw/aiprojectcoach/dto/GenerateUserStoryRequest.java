
package at.fhtw.aiprojectcoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// JSON the frontend sends to generate a user story with AI
public record GenerateUserStoryRequest(

        @NotBlank(message = "Requirement must not be empty")
        @Size(max = 2000, message = "Requirement must be at most 2000 characters")
        String requirement

) {
}
