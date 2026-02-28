package com.example.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core domain entity that represents the relationship between a
 * {@link Participant} and an {@link Event}.
 *
 * <p>{@code version} maps to the JPA {@code @Version} field in the persistence
 * adapter and enables optimistic locking to prevent concurrent over-enrollment.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Enrollment {

    private UUID id;
    private UUID eventId;
    private UUID participantId;

    /** Timestamp set at the moment of enrollment creation. */
    private LocalDateTime enrolledAt;

    /** Lifecycle state — ACTIVE or CANCELLED. */
    private EnrollmentStatus status;

    /**
     * Optimistic lock version. Mapped from {@code EnrollmentEntity.version}.
     * Never modified by domain logic; managed by the persistence adapter.
     */
    private Long version;
}
