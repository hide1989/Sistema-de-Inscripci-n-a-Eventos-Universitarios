package com.example.infrastructure.web.dto.response;

import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * HTTP response body for enrollment operations (CU-01, CU-02).
 * Built via Lombok {@code @Builder} — no direct setters exposed to callers.
 */
@Builder
public record EnrollmentResponse(
        UUID enrollmentId,
        UUID eventId,
        UUID participantId,
        LocalDateTime enrolledAt,
        EnrollmentStatus status
) {

    /** Convenience factory from the domain {@link Enrollment} object. */
    public static EnrollmentResponse fromDomain(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getId())
                .eventId(enrollment.getEventId())
                .participantId(enrollment.getParticipantId())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus())
                .build();
    }
}
