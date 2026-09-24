package com.codesentinel.review.controller;

import com.codesentinel.review.dto.ReviewRequest;
import com.codesentinel.review.dto.ReviewResponse;
import com.codesentinel.review.dto.PullRequestInput;
import com.codesentinel.review.dto.ReviewFileInput;
import com.codesentinel.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void shouldReturnReviewResponseForValidRequest() throws Exception {

        ReviewRequest request = new ReviewRequest(
                new PullRequestInput(
                        "tejasmudholkar1008/codesentinel-ai",
                        25,
                        "Add authentication",
                        "Adds authentication support",
                        "main",
                        "feature/authentication",
                        "abcdef1234567890",
                        "https://github.com/tejasmudholkar1008/codesentinel-ai/pull/25"
                ),
                List.of(
                        new ReviewFileInput(
                                "src/auth.ts",
                                "modified",
                                10,
                                2,
                                12,
                                "@@ -1,3 +1,11 @@\n+const token = getToken();"
                        )
                )
        );

        ReviewResponse response = new ReviewResponse(
                "COMPLETED",
                1,
                2,
                List.of()
        );

        when(reviewService.review(any(ReviewRequest.class)))
                .thenReturn(response);

        String validRequest = """
            {
              "pullRequest": {
                "repository": "tejasmudholkar1008/codesentinel-ai",
                "number": 25,
                "title": "Add authentication",
                "description": "Adds authentication support",
                "baseBranch": "main",
                "headBranch": "feature/authentication",
                "headSha": "abcdef1234567890",
                "url": "https://github.com/tejasmudholkar1008/codesentinel-ai/pull/25"
              },
              "files": [
                {
                  "filename": "src/auth.ts",
                  "status": "modified",
                  "additions": 10,
                  "deletions": 2,
                  "changes": 12,
                  "patch": "@@ -1,3 +1,11 @@\\n+const token = getToken();"
                }
              ]
            }
            """;

        mockMvc.perform(
                        post("/api/v1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.filesAnalyzed").value(1))
                .andExpect(jsonPath("$.issuesFound").value(2));

        verify(reviewService).review(any(ReviewRequest.class));
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest() throws Exception {

        String invalidRequest = """
                {
                  "pullRequest": {
                    "repository": "",
                    "number": 0,
                    "title": "",
                    "description": null,
                    "baseBranch": "",
                    "headBranch": "",
                    "headSha": "",
                    "url": ""
                  },
                  "files": []
                }
                """;

        mockMvc.perform(
                        post("/api/v1/reviews")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());
    }
}