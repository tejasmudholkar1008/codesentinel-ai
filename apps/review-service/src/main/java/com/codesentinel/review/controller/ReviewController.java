package com.codesentinel.review.controller;

import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import com.codesentinel.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> review(
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse response = reviewService.review(request);

        return ResponseEntity.ok(response);
    }
}