package com.codesentinel.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReviewRequest(

        @NotNull
        @Valid
        PullRequestInput pullRequest,

        @NotEmpty
        List<@Valid ReviewFileInput> files
) {
}