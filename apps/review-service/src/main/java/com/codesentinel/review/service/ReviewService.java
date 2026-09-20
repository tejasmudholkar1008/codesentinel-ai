package com.codesentinel.review.service;

import com.codesentinel.review.analyzer.ReviewAnalyzer;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewAnalyzer reviewAnalyzer;

    public ReviewService(
            @Qualifier("AIReviewAnalyzer") ReviewAnalyzer reviewAnalyzer
    ) {
        this.reviewAnalyzer = reviewAnalyzer;
    }

    public ReviewResponse review(ReviewRequest request) {

        List<ReviewFinding> findings =
                reviewAnalyzer.analyze(request);

        return new ReviewResponse(
                "COMPLETED",
                request.files().size(),
                findings.size(),
                findings
        );
    }
}