package com.codesentinel.review.service;

import com.codesentinel.review.analyzer.ReviewAnalyzer;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import com.codesentinel.review.review.Review;
import com.codesentinel.review.review.ReviewRepository;
import com.codesentinel.review.review.ReviewStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewAnalyzer reviewAnalyzer;
    private final ReviewRepository reviewRepository;

    public ReviewService(
            @Qualifier("AIReviewAnalyzer") ReviewAnalyzer reviewAnalyzer,
            ReviewRepository reviewRepository
    ) {
        this.reviewAnalyzer = reviewAnalyzer;
        this.reviewRepository = reviewRepository;
    }

    public ReviewResponse review(ReviewRequest request) {

        String repository = request.pullRequest().repository();
        Integer pullRequestNumber = request.pullRequest().number();
        String commitSha = request.pullRequest().headSha();

        var existingReview =
                reviewRepository
                        .findByRepositoryAndPullRequestNumberAndCommitSha(
                                repository,
                                pullRequestNumber,
                                commitSha
                        );

        if (existingReview.isPresent()) {

            Review existing = existingReview.get();

            if (existing.getStatus() == ReviewStatus.COMPLETED) {
                return new ReviewResponse(
                        "COMPLETED",
                        request.files().size(),
                        existing.getIssuesFound(),
                        List.of()
                );
            }

            if (existing.getStatus() == ReviewStatus.IN_PROGRESS) {
                return new ReviewResponse(
                        "IN_PROGRESS",
                        request.files().size(),
                        0,
                        List.of()
                );
            }

            // FAILED → retry using the existing Review entity.
            existing.retry();
            reviewRepository.save(existing);

            return executeReview(existing, request);
        }

        Review review = new Review(
                repository,
                pullRequestNumber,
                commitSha
        );

        reviewRepository.save(review);

        return executeReview(review, request);
    }

    private ReviewResponse executeReview(
            Review review,
            ReviewRequest request
    ) {
        try {

            List<ReviewFinding> findings =
                    reviewAnalyzer.analyze(request);

            review.complete(findings.size());

            reviewRepository.save(review);

            return new ReviewResponse(
                    "COMPLETED",
                    request.files().size(),
                    findings.size(),
                    findings
            );

        } catch (RuntimeException exception) {

            review.fail();

            reviewRepository.save(review);

            throw exception;
        }
    }
}