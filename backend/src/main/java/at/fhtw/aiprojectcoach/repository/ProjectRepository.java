
package at.fhtw.aiprojectcoach.repository;

import at.fhtw.aiprojectcoach.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {
}
