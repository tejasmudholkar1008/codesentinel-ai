package com.codesentinel.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewFileInput(

        @NotBlank
        String filename,

        @NotBlank
        String status,

        int additions,

        int deletions,

        int changes,

        String patch
) {
}