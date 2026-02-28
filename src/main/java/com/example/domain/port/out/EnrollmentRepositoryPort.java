package com.example.domain.port.out;

import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port — Enrollment persistence abstraction.
 *
 * <p>Implemented by {@code EnrollmentJpaAdapter} in the infrastructure layer.
 * Exposes the minimum surface needed by the application use cases.
 */
public interface EnrollmentRepositoryPort {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(UUID id);

    /**
     * Returns all enrollments for the given event filtered by status.
     * Used by CU-04 (list participants) and CU-02 (cancel — verify active state).
     */
    List<Enrollment> findByEventIdAndStatus(UUID eventId, EnrollmentStatus status);

    /**
     * Idempotency check: returns {@code true} if the participant already has
     * an active enrollment for the event, preventing duplicate registrations.
     */
    boolean existsByEventIdAndParticipantIdAndStatus(UUID eventId, UUID participantId,
                                                      EnrollmentStatus status);
}
