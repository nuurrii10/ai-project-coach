
package at.fhtw.aiprojectcoach.service;

import at.fhtw.aiprojectcoach.entity.Project;
import at.fhtw.aiprojectcoach.entity.Sprint;
import at.fhtw.aiprojectcoach.entity.UserStory;
import at.fhtw.aiprojectcoach.repository.ProjectRepository;
import at.fhtw.aiprojectcoach.repository.SprintRepository;
import at.fhtw.aiprojectcoach.repository.UserStoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserStoryRepository userStoryRepository;

    public SprintService(
            SprintRepository sprintRepository,
            ProjectRepository projectRepository,
            UserStoryRepository userStoryRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.userStoryRepository = userStoryRepository;
    }

    @Transactional
    public Sprint createSprint(
            Long projectId,
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Sprint name is required");
        }

        if (startDate != null && endDate != null
                && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid sprint dates");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        Sprint sprint = new Sprint(name, startDate, endDate, project);

        return sprintRepository.save(sprint);
    }

    @Transactional
    public void assignUserStory(Long sprintId, Long userStoryId) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sprint not found"));

        UserStory story = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User story not found"));

        if (!Objects.equals(
                sprint.getProject().getId(),
                story.getProject().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint and user story must belong to the same project");
        }

        story.setSprint(sprint);
        userStoryRepository.save(story);
    }
}
