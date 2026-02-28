package com.example.application.functional;

import com.example.domain.exception.DuplicateEnrollmentException;
import com.example.domain.exception.EventFullException;
import com.example.domain.exception.EventNotFoundException;
import com.example.domain.exception.ParticipantNotFoundException;
import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.model.Event;
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
 * Functional (declarative) implementation of {@link EnrollParticipantPort} (CU-01).
 *
 * <p>Achieves the same business outcome as
 * {@link com.example.application.imperative.ImperativeEnrollmentService}
 * but replaces explicit {@code if} blocks with {@code Optional.orElseThrow},
 * {@code Optional.filter}, and method references.
 *
 * <p>The transaction contract is identical: {@code SERIALIZABLE} isolation
 * with {@code @Version}-based optimistic locking as the safety net.
 */
@RequiredArgsConstructor
public class FunctionalEnrollmentService implements EnrollParticipantPort {

    private final EventRepositoryPort eventRepository;
    private final ParticipantRepositoryPort participantRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final AuditEventPublisherPort auditPublisher;

    /**
     * {@inheritDoc}
     *
     * <p><b>Functional flow:</b> chains of {@code Optional.orElseThrow},
     * {@code filter} for capacity and duplicate checks, and {@code Builder}
     * pattern for object construction — no mutable local variables except
     * the persisted domain objects.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Enrollment enroll(UUID eventId, UUID participantId) {

        // 1. Resolve event — functional Optional chain
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. Resolve participant
        participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        // 3. Idempotency: guard via predicate
        if (enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)) {
            throw new DuplicateEnrollmentException(eventId, participantId);
        }

        // 4. Capacity guard via Optional.filter
        Event availableEvent = java.util.Optional.of(event)
                .filter(e -> e.getAvailableSpots() > 0)
                .orElseThrow(() -> new EventFullException(eventId));

        // 5. Decrement spots using builder (functional reconstruction — preserve version for optimistic lock)
        Event updatedEvent = Event.builder()
                .id(availableEvent.getId())
                .name(availableEvent.getName())
                .maxCapacity(availableEvent.getMaxCapacity())
                .availableSpots(availableEvent.getAvailableSpots() - 1)
                .eventDate(availableEvent.getEventDate())
                .version(availableEvent.getVersion())
                .build();
        eventRepository.save(updatedEvent);

        // 6. Build and persist enrollment via Builder
        Enrollment enrollment = Enrollment.builder()
                .eventId(eventId)
                .participantId(participantId)
                .enrolledAt(LocalDateTime.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();
        Enrollment saved = enrollmentRepository.save(enrollment);

        // 7. Publish audit event
        auditPublisher.publish(new EnrollmentCreatedAuditEvent(
                eventId, participantId, updatedEvent.getAvailableSpots(), LocalDateTime.now()));

        return saved;
    }
}
