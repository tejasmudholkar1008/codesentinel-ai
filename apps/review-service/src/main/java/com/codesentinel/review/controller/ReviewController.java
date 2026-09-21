package com.codesentinel.review.controller;

import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import com.codesentinel.review.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private static final Logger log =
            LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> review(
            @Valid @RequestBody ReviewRequest request
    ) {
        log.info(
                "Review request received: prNumber={}, files={}",
                request.pullRequest().number(),
                request.files().size()
        );

        ReviewResponse response = reviewService.review(request);

        log.info(
                "Review completed: prNumber={}, filesAnalyzed={}, issuesFound={}",
                request.pullRequest().number(),
                response.filesAnalyzed(),
                response.issuesFound()
        );

        return ResponseEntity.ok(response);
    }
}