package com.example.application.imperative;

import com.example.domain.exception.DuplicateEnrollmentException;
import com.example.domain.exception.EventFullException;
import com.example.domain.exception.EventNotFoundException;
import com.example.domain.exception.ParticipantNotFoundException;
import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.model.Event;
import com.example.domain.model.Participant;
import com.example.domain.port.in.EnrollParticipantPort;
import com.example.domain.port.out.AuditEventPublisherPort;
import com.example.domain.port.out.EnrollmentRepositoryPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.domain.port.out.ParticipantRepositoryPort;
import com.example.infrastructure.messaging.event.EnrollmentCreatedAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Imperative implementation of {@link EnrollParticipantPort} (CU-01).
 *
 * <p>Uses explicit conditional blocks and variable assignments to make the
 * control flow visible. Compare with {@link com.example.application.functional.FunctionalEnrollmentService}
 * which achieves the same result using {@code Optional} chains and lambdas.
 *
 * <p>The transaction uses {@code SERIALIZABLE} isolation to prevent concurrent
 * threads from reading the same {@code availableSpots} value and both succeeding.
 * An {@code OptimisticLockException} on the JPA {@code @Version} field acts as
 * a safety net for any race condition that serializable isolation does not catch
 * at the H2 level.
 */
@RequiredArgsConstructor
public class ImperativeEnrollmentService implements EnrollParticipantPort {

    private final EventRepositoryPort eventRepository;
    private final ParticipantRepositoryPort participantRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final AuditEventPublisherPort auditPublisher;

    /**
     * {@inheritDoc}
     *
     * <p><b>Imperative flow:</b>
     * <ol>
     *   <li>Look up event — throw {@link EventNotFoundException} if absent.
     *   <li>Look up participant — throw {@link ParticipantNotFoundException} if absent.
     *   <li>Idempotency check — throw {@link DuplicateEnrollmentException} if already enrolled.
     *   <li>Capacity check — throw {@link EventFullException} if no spots left.
     *   <li>Decrement {@code availableSpots} and persist updated event.
     *   <li>Persist new enrollment with {@code ACTIVE} status.
     *   <li>Publish audit event.
     * </ol>
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Enrollment enroll(UUID eventId, UUID participantId) {

        // 1. Validate event existence
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. Validate participant existence
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        // 3. Idempotency: prevent duplicate active enrollment
        if (enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)) {
            throw new DuplicateEnrollmentException(eventId, participantId);
        }

        // 4. Capacity check
        if (event.getAvailableSpots() <= 0) {
            throw new EventFullException(eventId);
        }

        // 5. Decrement spots and persist event
        event.setAvailableSpots(event.getAvailableSpots() - 1);
        eventRepository.save(event);

        // 6. Persist enrollment
        Enrollment enrollment = Enrollment.builder()
                .eventId(event.getId())
                .participantId(participant.getId())
                .enrolledAt(LocalDateTime.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();
        Enrollment saved = enrollmentRepository.save(enrollment);

        // 7. Publish audit event
        auditPublisher.publish(new EnrollmentCreatedAuditEvent(
                eventId, participantId, event.getAvailableSpots(), LocalDateTime.now()));

        return saved;
    }
}
