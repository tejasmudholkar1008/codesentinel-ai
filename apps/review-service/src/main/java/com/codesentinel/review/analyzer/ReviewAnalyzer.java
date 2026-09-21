package com.codesentinel.review.analyzer;

import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;

import java.util.List;

public interface ReviewAnalyzer {

    List<ReviewFinding> analyze(ReviewRequest request);
}
