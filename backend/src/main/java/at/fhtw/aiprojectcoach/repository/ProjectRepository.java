package at.fhtw.aiprojectcoach.repository;

import at.fhtw.aiprojectcoach.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data creates save, findAll, findById, ... automatically
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
