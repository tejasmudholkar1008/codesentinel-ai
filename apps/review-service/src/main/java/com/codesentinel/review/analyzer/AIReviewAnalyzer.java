package com.codesentinel.review.analyzer;

import com.codesentinel.review.ai.AIProvider;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIReviewAnalyzer implements ReviewAnalyzer {

    private final AIProvider aiProvider;

    public AIReviewAnalyzer(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public List<ReviewFinding> analyze(ReviewRequest request) {
        return aiProvider.analyze(request);
    }
}