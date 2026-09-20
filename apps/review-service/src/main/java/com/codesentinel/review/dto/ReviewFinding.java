package com.codesentinel.review.dto;

public record ReviewFinding(
        String severity,
        String category,
        String filename,
        Integer line,
        String title,
        String description,
        String suggestion
) {
}