
package at.fhtw.aiprojectcoach.repository;

import at.fhtw.aiprojectcoach.entity.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStoryRepository
        extends JpaRepository<UserStory, Long> {

    List<UserStory> findAllByProject_IdOrderByCreatedAtDesc(Long projectId);
}
