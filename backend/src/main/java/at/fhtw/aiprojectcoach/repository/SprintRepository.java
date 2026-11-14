package at.fhtw.aiprojectcoach.repository;

import at.fhtw.aiprojectcoach.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findAllByProject_IdOrderByStartDateAsc(Long projectId);
}
