package com.codesentinel.review.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void shouldSaveAndFindReviewByRepositoryPullRequestAndCommitSha() {
        Review review = new Review(
                "test-owner/test-repo",
                14,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );

        Review savedReview = reviewRepository.saveAndFlush(review);

        assertThat(savedReview.getId()).isNotNull();
        assertThat(savedReview.getRepository())
                .isEqualTo("test-owner/test-repo");
        assertThat(savedReview.getPullRequestNumber())
                .isEqualTo(14);
        assertThat(savedReview.getCommitSha())
                .isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(savedReview.getStatus())
                .isEqualTo(ReviewStatus.IN_PROGRESS);
        assertThat(savedReview.getIssuesFound())
                .isZero();

        var foundReview =
                reviewRepository
                        .findByRepositoryAndPullRequestNumberAndCommitSha(
                                "test-owner/test-repo",
                                14,
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        );

        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getId())
                .isEqualTo(savedReview.getId());
    }

    @Test
    void shouldAllowSamePullRequestWithDifferentCommitSha() {
        Review firstReview = new Review(
                "test-owner/test-repo",
                14,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );

        Review secondReview = new Review(
                "test-owner/test-repo",
                14,
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        );

        reviewRepository.saveAndFlush(firstReview);
        reviewRepository.saveAndFlush(secondReview);

        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateRepositoryPullRequestAndCommitSha() {
        Review firstReview = new Review(
                "test-owner/test-repo",
                14,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );

        Review duplicateReview = new Review(
                "test-owner/test-repo",
                14,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );

        reviewRepository.saveAndFlush(firstReview);

        assertThatThrownBy(() ->
                reviewRepository.saveAndFlush(duplicateReview)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}