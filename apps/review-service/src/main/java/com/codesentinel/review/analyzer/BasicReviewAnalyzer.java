package com.codesentinel.review.analyzer;

import com.codesentinel.review.dto.ReviewFileInput;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BasicReviewAnalyzer implements ReviewAnalyzer {

    @Override
    public List<ReviewFinding> analyze(ReviewRequest request) {

        List<ReviewFinding> findings = new ArrayList<>();

        for (ReviewFileInput file : request.files()) {

            if (file.patch() == null || file.patch().isBlank()) {
                continue;
            }

            if (file.patch().contains("console.log")) {
                findings.add(
                        new ReviewFinding(
                                "LOW",
                                "CODE_QUALITY",
                                file.filename(),
                                null,
                                "Console logging detected",
                                "Console logging was detected in the changed code.",
                                "Remove unnecessary console logging before merging."
                        )
                );
            }
        }

        return findings;
    }
}