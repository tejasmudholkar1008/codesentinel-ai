package com.codesentinel.review.dto;

import java.util.List;

public record ReviewResponse(
        String status,
        int filesAnalyzed,
        int issuesFound,
        List<ReviewFinding> findings
) {
}