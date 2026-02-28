package com.example.infrastructure.persistence.repository;

import com.example.domain.model.EnrollmentStatus;
import com.example.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link EnrollmentEntity}. Internal to the persistence adapter. */
public interface SpringEnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {

    /**
     * Finds all enrollments for the given event filtered by status.
     * Used by CU-04 (list active participants) and CU-02 (verify active state before cancel).
     */
    List<EnrollmentEntity> findByEventIdAndStatus(UUID eventId, EnrollmentStatus status);

    /**
     * Idempotency check for CU-01: verifies whether a participant already
     * holds an active enrollment for the event before attempting to create one.
     */
    boolean existsByEventIdAndParticipantIdAndStatus(UUID eventId, UUID participantId,
                                                      EnrollmentStatus status);
}
