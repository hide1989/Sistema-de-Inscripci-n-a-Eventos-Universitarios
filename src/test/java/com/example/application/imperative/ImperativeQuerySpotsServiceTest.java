package com.example.application.imperative;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.model.Event;
import com.example.domain.port.out.AuditEventPublisherPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.infrastructure.messaging.event.SpotsQueriedAuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImperativeQuerySpotsService — Unit Tests")
class ImperativeQuerySpotsServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private AuditEventPublisherPort auditPublisher;

    @InjectMocks
    private ImperativeQuerySpotsService service;

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an existing event with 7 available spots, " +
            "When querySpots is called, " +
            "Then the event is returned with availableSpots = 7")
    void shouldReturnEventWithCorrectAvailableSpots() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).name("Taller").maxCapacity(10)
                .availableSpots(7).eventDate(LocalDateTime.now().plusDays(1)).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When
        Event result = service.querySpots(eventId);

        // Then
        assertThat(result.getAvailableSpots()).isEqualTo(7);
        assertThat(result.getId()).isEqualTo(eventId);
        verify(auditPublisher).publish(any(SpotsQueriedAuditEvent.class));
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a non-existent event UUID, " +
            "When querySpots is called, " +
            "Then EventNotFoundException is thrown")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.querySpots(eventId))
                .isInstanceOf(EventNotFoundException.class);

        verify(auditPublisher, never()).publish(any());
    }
}
