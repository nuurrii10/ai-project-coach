package at.fhtw.aiprojectcoach.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_stories")
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "As a student, I want ..., so that ..."
    @Column(nullable = false, length = 1000)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private Integer storyPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoryStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Many user stories belong to one project (column "project_id" in the database)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // JPA needs an empty constructor
    protected UserStory() {
    }

    public UserStory(String title, Priority priority, Integer storyPoints, Project project) {
        this.title = title;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.project = project;
        this.status = StoryStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Priority getPriority() {
        return priority;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public StoryStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Project getProject() {
        return project;
    }
}
