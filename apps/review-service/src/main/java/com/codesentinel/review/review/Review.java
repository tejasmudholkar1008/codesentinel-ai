package com.codesentinel.review.review;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_repository_pr_commit",
                        columnNames = {
                                "repository",
                                "pull_request_number",
                                "commit_sha"
                        }
                )
        }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repository;

    @Column(name = "pull_request_number", nullable = false)
    private Integer pullRequestNumber;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(name = "issues_found", nullable = false)
    private Integer issuesFound;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {
    }

    public Review(
            String repository,
            Integer pullRequestNumber,
            String commitSha
    ) {
        this.repository = repository;
        this.pullRequestNumber = pullRequestNumber;
        this.commitSha = commitSha;
        this.status = ReviewStatus.IN_PROGRESS;
        this.issuesFound = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public Integer getIssuesFound() {
        return issuesFound;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void complete(int issuesFound) {
        this.status = ReviewStatus.COMPLETED;
        this.issuesFound = issuesFound;
    }

    public void retry() {
        this.status = ReviewStatus.IN_PROGRESS;
        this.issuesFound = 0;
    }

    public void fail() {
        this.status = ReviewStatus.FAILED;
    }
}

