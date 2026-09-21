package com.codesentinel.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PullRequestInput(

        @Positive
        int number,

        @NotBlank
        String title,

        String description,

        @NotBlank
        String baseBranch,

        @NotBlank
        String headBranch,

        @NotBlank
        String headSha,

        @NotBlank
        String url
) {
}