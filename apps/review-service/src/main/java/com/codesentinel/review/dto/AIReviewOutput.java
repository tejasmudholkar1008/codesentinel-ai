package com.codesentinel.review.dto;

import java.util.List;

public class AIReviewOutput {

    public List<AIReviewFinding> findings;

    public static class AIReviewFinding {

        public String severity;
        public String category;
        public String filename;
        public Integer line;
        public String title;
        public String description;
        public String suggestion;
    }
}