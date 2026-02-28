package com.example.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * HTTP request body for enrollment creation endpoints (CU-01).
 *
 * <p>Validated by Spring's {@code @Valid} before reaching the controller method.
 */
@Builder
public record EnrollmentRequest(

        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotNull(message = "participantId is required")
        UUID participantId
) {
}
