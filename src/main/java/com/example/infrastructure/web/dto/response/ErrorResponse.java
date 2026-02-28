package com.example.infrastructure.web.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Unified HTTP error response body for all domain and validation exceptions.
 * All error mapping is centralised in {@code GlobalExceptionHandler} (SRP).
 */
@Builder
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path
) {

    /**
     * Factory method for clean one-liner construction in the exception handler.
     */
    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
