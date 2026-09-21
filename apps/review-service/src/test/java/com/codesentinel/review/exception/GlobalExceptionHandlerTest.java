package com.codesentinel.review.exception;

import com.codesentinel.review.ai.AIProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnServiceUnavailableForAIProviderException() {

        AIProviderException exception =
                new AIProviderException("AI code analysis failed");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAIProviderException(exception);

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );

        assertEquals(
                "AI_PROVIDER_ERROR",
                response.getBody().code()
        );

        assertEquals(
                "Unable to complete code analysis.",
                response.getBody().message()
        );
    }
}