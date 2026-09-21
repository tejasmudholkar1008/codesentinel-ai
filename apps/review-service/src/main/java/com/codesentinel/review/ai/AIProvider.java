package com.codesentinel.review.ai;

import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewRequest;

import java.util.List;

public interface AIProvider {

    List<ReviewFinding> analyze(ReviewRequest request);
}