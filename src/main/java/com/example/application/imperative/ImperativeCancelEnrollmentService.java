package com.example.application.imperative;

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
 * Imperative implementation of {@link CancelEnrollmentPort} (CU-02).
 *
 * <p>Cancels an active enrollment and restores the event spot in a single
 * atomic transaction. Uses explicit null/presence checks and setter mutations.
 */
@RequiredArgsConstructor
public class ImperativeCancelEnrollmentService implements CancelEnrollmentPort {

    private final EventRepositoryPort eventRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final AuditEventPublisherPort auditPublisher;

    @Override
    @Transactional
    public void cancel(UUID enrollmentId) {

        // 1. Locate enrollment
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        // 2. Mark as cancelled
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);

        // 3. Restore spot on the event
        Event event = eventRepository.findById(enrollment.getEventId())
                .orElseThrow(() -> new EventNotFoundException(enrollment.getEventId()));

        event.setAvailableSpots(event.getAvailableSpots() + 1);
        eventRepository.save(event);

        // 4. Publish audit event
        auditPublisher.publish(new EnrollmentCancelledAuditEvent(
                enrollmentId, enrollment.getEventId(), event.getAvailableSpots(), LocalDateTime.now()));
    }
}
