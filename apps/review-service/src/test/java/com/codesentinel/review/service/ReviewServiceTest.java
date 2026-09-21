package com.codesentinel.review.service;

import com.codesentinel.review.ai.AIProviderException;
import com.codesentinel.review.analyzer.ReviewAnalyzer;
import com.codesentinel.review.dto.PullRequestInput;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewFileInput;
import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Test
    void shouldReturnCompletedReviewWhenAnalysisSucceeds() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);

        ReviewFinding finding = new ReviewFinding(
                "HIGH",
                "SECURITY",
                "src/auth.ts",
                14,
                "Sensitive token logged",
                "Authentication token is logged.",
                "Remove the token from logs."
        );

        when(reviewAnalyzer.analyze(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(finding));

        ReviewService reviewService = new ReviewService(reviewAnalyzer);

        ReviewRequest request = createReviewRequest();

        ReviewResponse response = reviewService.review(request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.filesAnalyzed());
        assertEquals(1, response.issuesFound());
        assertEquals(1, response.findings().size());
        assertEquals("SECURITY", response.findings().getFirst().category());
    }

    @Test
    void shouldPropagateAIProviderExceptionWhenAnalysisFails() {

        ReviewAnalyzer reviewAnalyzer = mock(ReviewAnalyzer.class);

        when(reviewAnalyzer.analyze(org.mockito.ArgumentMatchers.any()))
                .thenThrow(
                        new AIProviderException("AI code analysis failed")
                );

        ReviewService reviewService = new ReviewService(reviewAnalyzer);

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

    private ReviewRequest createReviewRequest() {

        PullRequestInput pullRequest = new PullRequestInput(
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