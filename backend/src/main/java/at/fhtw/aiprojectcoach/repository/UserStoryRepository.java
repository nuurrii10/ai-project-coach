package at.fhtw.aiprojectcoach.repository;

import at.fhtw.aiprojectcoach.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    // Spring builds the query from the method name: WHERE project_id = ?
    List<UserStory> findByProjectId(Long projectId);
}
