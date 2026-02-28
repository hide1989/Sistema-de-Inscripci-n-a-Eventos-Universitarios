package com.example.application.functional;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.model.Event;
import com.example.domain.port.in.QueryAvailableSpotsPort;
import com.example.domain.port.out.AuditEventPublisherPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.infrastructure.messaging.event.SpotsQueriedAuditEvent;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Functional implementation of {@link QueryAvailableSpotsPort} (CU-03).
 *
 * <p>Uses a single {@code Optional.orElseThrow} chain — no intermediate
 * variables or if-else branches.
 */
@RequiredArgsConstructor
public class FunctionalQuerySpotsService implements QueryAvailableSpotsPort {

    private final EventRepositoryPort eventRepository;
    private final AuditEventPublisherPort auditPublisher;

    @Override
    public Event querySpots(UUID eventId) {

        // Single-expression resolution — functional style
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        auditPublisher.publish(new SpotsQueriedAuditEvent(
                eventId, event.getAvailableSpots(), LocalDateTime.now()));

        return event;
    }
}
