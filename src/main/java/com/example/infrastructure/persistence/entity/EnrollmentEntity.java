package com.example.infrastructure.persistence.entity;

import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code enrollments} table.
 *
 * <p>The {@code @Version} field enables optimistic locking: if two concurrent
 * transactions attempt to update the same enrollment row, one will succeed and
 * the other will receive an {@code OptimisticLockException}, translated to
 * HTTP 409 by {@code GlobalExceptionHandler}.
 *
 * <p>The combination of {@code eventId + participantId} is unique for ACTIVE
 * enrollments, enforced at the service layer via idempotency check.
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrollmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID participantId;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    /** Optimistic locking — managed exclusively by JPA, not by domain logic. */
    @Version
    private Long version;

    // ── Mapping ───────────────────────────────────────────────────────────────

    public Enrollment toDomain() {
        return Enrollment.builder()
                .id(id)
                .eventId(eventId)
                .participantId(participantId)
                .enrolledAt(enrolledAt)
                .status(status)
                .version(version)
                .build();
    }

    public static EnrollmentEntity fromDomain(Enrollment enrollment) {
        return EnrollmentEntity.builder()
                .id(enrollment.getId())
                .eventId(enrollment.getEventId())
                .participantId(enrollment.getParticipantId())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus())
                .version(enrollment.getVersion())
                .build();
    }
}
