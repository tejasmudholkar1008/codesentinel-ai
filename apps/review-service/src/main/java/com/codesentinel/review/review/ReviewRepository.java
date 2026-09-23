package com.codesentinel.review.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByRepositoryAndPullRequestNumberAndCommitSha(
            String repository,
            Integer pullRequestNumber,
            String commitSha
    );
}