package com.codesentinel.review.service;

import com.codesentinel.review.ai.AIProviderException;
import com.codesentinel.review.analyzer.ReviewAnalyzer;
import com.codesentinel.review.dto.PullRequestInput;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewFileInput;
import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import com.codesentinel.review.review.Review;
import com.codesentinel.review.review.ReviewRepository;
import com.codesentinel.review.review.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Test
    void shouldReturnCompletedReviewWhenAnalysisSucceeds() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.empty());

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewFinding finding = new ReviewFinding(
                "HIGH",
                "SECURITY",
                "src/auth.ts",
                14,
                "Sensitive token logged",
                "Authentication token is logged.",
                "Remove the token from logs."
        );

        when(reviewAnalyzer.analyze(any()))
                .thenReturn(List.of(finding));

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.filesAnalyzed());
        assertEquals(1, response.issuesFound());
        assertEquals(1, response.findings().size());
        assertEquals(
                "SECURITY",
                response.findings().getFirst().category()
        );
    }

    @Test
    void shouldPropagateAIProviderExceptionWhenAnalysisFails() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.empty());

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(reviewAnalyzer.analyze(any()))
                .thenThrow(
                        new AIProviderException("AI code analysis failed")
                );

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        AIProviderException exception = assertThrows(
                AIProviderException.class,
                () -> reviewService.review(request)
        );

        assertEquals(
                "AI code analysis failed",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateAndCompleteReviewWhenCommitHasNotBeenReviewed() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        ReviewFinding finding = new ReviewFinding(
                "HIGH",
                "SECURITY",
                "src/auth.ts",
                14,
                "Sensitive token logged",
                "Authentication token is logged.",
                "Remove the token from logs."
        );

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.empty());

        when(reviewAnalyzer.analyze(any()))
                .thenReturn(List.of(finding));

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.issuesFound());

        verify(reviewRepository)
                .findByRepositoryAndPullRequestNumberAndCommitSha(
                        "example-repo",
                        123,
                        "abc123"
                );

        verify(reviewRepository, times(2))
                .save(any(Review.class));

        verify(reviewAnalyzer)
                .analyze(request);
    }

    @Test
    void shouldSkipAnalysisWhenReviewAlreadyCompleted() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        Review existingReview = new Review(
                "example-repo",
                123,
                "abc123"
        );

        existingReview.complete(1);

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.of(existingReview));

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.issuesFound());
        assertTrue(response.findings().isEmpty());

        verify(reviewRepository)
                .findByRepositoryAndPullRequestNumberAndCommitSha(
                        "example-repo",
                        123,
                        "abc123"
                );

        verify(reviewAnalyzer, never())
                .analyze(any());

        verify(reviewRepository, never())
                .save(any(Review.class));
    }

    @Test
    void shouldMarkReviewAsFailedWhenAnalysisFails() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.empty());

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(reviewAnalyzer.analyze(any()))
                .thenThrow(
                        new AIProviderException("AI code analysis failed")
                );

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        AIProviderException exception = assertThrows(
                AIProviderException.class,
                () -> reviewService.review(request)
        );

        assertEquals(
                "AI code analysis failed",
                exception.getMessage()
        );

        verify(reviewRepository, times(2))
                .save(any(Review.class));
    }

    @Test
    void shouldSkipAnalysisWhenReviewAlreadyInProgress() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        Review existingReview = new Review(
                "example-repo",
                123,
                "abc123"
        );

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.of(existingReview));

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("IN_PROGRESS", response.status());

        verify(reviewRepository)
                .findByRepositoryAndPullRequestNumberAndCommitSha(
                        "example-repo",
                        123,
                        "abc123"
                );

        verify(reviewAnalyzer, never())
                .analyze(any());

        verify(reviewRepository, never())
                .save(any(Review.class));
    }

    @Test
    void shouldRetryReviewWhenPreviousAttemptFailed() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        Review existingReview = new Review(
                "example-repo",
                123,
                "abc123"
        );

        existingReview.fail();

        ReviewFinding finding = new ReviewFinding(
                "HIGH",
                "SECURITY",
                "src/auth.ts",
                14,
                "Sensitive token logged",
                "Authentication token is logged.",
                "Remove the token from logs."
        );

        when(reviewRepository.findByRepositoryAndPullRequestNumberAndCommitSha(
                "example-repo",
                123,
                "abc123"
        )).thenReturn(Optional.of(existingReview));

        when(reviewAnalyzer.analyze(any()))
                .thenReturn(List.of(finding));

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewService reviewService =
                new ReviewService(reviewAnalyzer, reviewRepository);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.issuesFound());

        verify(reviewAnalyzer)
                .analyze(request);

        verify(reviewRepository, times(2))
                .save(existingReview);

        assertEquals(
                ReviewStatus.COMPLETED,
                existingReview.getStatus()
        );

        assertEquals(ReviewStatus.COMPLETED, existingReview.getStatus());
        assertEquals(1, existingReview.getIssuesFound());
    }

    private ReviewRequest createReviewRequest() {

        PullRequestInput pullRequest = new PullRequestInput(
                "example-repo",
                123,
                "Add authentication",
                "Adds authentication support.",
                "main",
                "feature/auth",
                "abc123",
                "https://github.com/example/repo/pull/123"
        );

        ReviewFileInput file = new ReviewFileInput(
                "src/auth.ts",
                "modified",
                10,
                2,
                12,
                "@@ -1,5 +1,13 @@"
        );

        return new ReviewRequest(
                pullRequest,
                List.of(file)
        );
    }
}