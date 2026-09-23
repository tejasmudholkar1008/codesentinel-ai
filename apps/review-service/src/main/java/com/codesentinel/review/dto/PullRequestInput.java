package com.codesentinel.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PullRequestInput(

        @NotBlank
        String repository,

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