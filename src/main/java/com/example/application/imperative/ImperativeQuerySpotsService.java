package com.example.application.imperative;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.model.Event;
import com.example.domain.port.in.QueryAvailableSpotsPort;
import com.example.domain.port.out.AuditEventPublisherPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.infrastructure.messaging.event.SpotsQueriedAuditEvent;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Imperative implementation of {@link QueryAvailableSpotsPort} (CU-03).
 *
 * <p>Read-only operation: no transaction annotation needed. Uses explicit
 * {@code Optional} presence check with if-else.
 */
@RequiredArgsConstructor
public class ImperativeQuerySpotsService implements QueryAvailableSpotsPort {

    private final EventRepositoryPort eventRepository;
    private final AuditEventPublisherPort auditPublisher;

    @Override
    public Event querySpots(UUID eventId) {

        // Explicit presence check — imperative style
        Optional<Event> result = eventRepository.findById(eventId);
        if (result.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Event event = result.get();

        auditPublisher.publish(new SpotsQueriedAuditEvent(
                eventId, event.getAvailableSpots(), LocalDateTime.now()));

        return event;
    }
}
