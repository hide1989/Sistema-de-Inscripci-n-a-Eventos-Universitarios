package com.example.application.functional;

import com.example.domain.exception.EnrollmentNotFoundException;
import com.example.domain.exception.EventNotFoundException;
import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.model.Event;
import com.example.domain.port.in.CancelEnrollmentPort;
import com.example.domain.port.out.AuditEventPublisherPort;
import com.example.domain.port.out.EnrollmentRepositoryPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.infrastructure.messaging.event.EnrollmentCancelledAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Functional implementation of {@link CancelEnrollmentPort} (CU-02).
 *
 * <p>Uses {@code Optional.orElseThrow} for presence checks and the Builder
 * pattern to create updated domain objects without mutating existing state.
 */
@RequiredArgsConstructor
public class FunctionalCancelEnrollmentService implements CancelEnrollmentPort {

    private final EventRepositoryPort eventRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final AuditEventPublisherPort auditPublisher;

    @Override
    @Transactional
    public void cancel(UUID enrollmentId) {

        // 1. Resolve enrollment — functional Optional chain
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        // 2. Build cancelled enrollment (immutable reconstruction via Builder)
        Enrollment cancelled = Enrollment.builder()
                .id(enrollment.getId())
                .eventId(enrollment.getEventId())
                .participantId(enrollment.getParticipantId())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(EnrollmentStatus.CANCELLED)
                .version(enrollment.getVersion())
                .build();
        enrollmentRepository.save(cancelled);

        // 3. Restore spot — immutable event reconstruction
        Event event = eventRepository.findById(enrollment.getEventId())
                .orElseThrow(() -> new EventNotFoundException(enrollment.getEventId()));

        Event restored = Event.builder()
                .id(event.getId())
                .name(event.getName())
                .maxCapacity(event.getMaxCapacity())
                .availableSpots(event.getAvailableSpots() + 1)
                .eventDate(event.getEventDate())
                .version(event.getVersion())
                .build();
        eventRepository.save(restored);

        // 4. Publish audit event
        auditPublisher.publish(new EnrollmentCancelledAuditEvent(
                enrollmentId, enrollment.getEventId(), restored.getAvailableSpots(), LocalDateTime.now()));
    }
}
