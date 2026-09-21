package com.codesentinel.review.exception;

import com.codesentinel.review.ai.AIProviderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<ErrorResponse> handleAIProviderException(
            AIProviderException exception
    ) {

        ErrorResponse response = new ErrorResponse(
                "AI_PROVIDER_ERROR",
                "Unable to complete code analysis."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    public record ErrorResponse(
            String code,
            String message
    ) {
    }
}