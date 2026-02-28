package com.example.application.imperative;

import com.example.domain.exception.EnrollmentNotFoundException;
import com.example.domain.model.*;
import com.example.domain.port.out.*;
import com.example.infrastructure.messaging.event.EnrollmentCancelledAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImperativeCancelEnrollmentService — Unit Tests")
class ImperativeCancelEnrollmentServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private AuditEventPublisherPort auditPublisher;

    @InjectMocks
    private ImperativeCancelEnrollmentService service;

    private UUID enrollmentId;
    private UUID eventId;
    private Enrollment activeEnrollment;
    private Event event;

    @BeforeEach
    void setUp() {
        enrollmentId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        activeEnrollment = Enrollment.builder()
                .id(enrollmentId).eventId(eventId).participantId(UUID.randomUUID())
                .enrolledAt(LocalDateTime.now()).status(EnrollmentStatus.ACTIVE).build();

        event = Event.builder()
                .id(eventId).name("Test Event")
                .maxCapacity(10).availableSpots(7)
                .eventDate(LocalDateTime.now().plusDays(1)).build();
    }

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an active enrollment, " +
            "When cancel is called, " +
            "Then status is set to CANCELLED and event spots are incremented")
    void shouldCancelEnrollmentAndRestoreSpot() {
        // Given
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(activeEnrollment));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(enrollmentRepository.save(any())).thenReturn(activeEnrollment);
        when(eventRepository.save(any())).thenReturn(event);

        // When
        service.cancel(enrollmentId);

        // Then
        verify(enrollmentRepository).save(argThat(e -> e.getStatus() == EnrollmentStatus.CANCELLED));
        verify(eventRepository).save(argThat(e -> e.getAvailableSpots() == 8));
        verify(auditPublisher).publish(any(EnrollmentCancelledAuditEvent.class));
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an enrollment UUID that does not exist, " +
            "When cancel is called, " +
            "Then EnrollmentNotFoundException is thrown")
    void shouldThrowEnrollmentNotFoundExceptionWhenEnrollmentDoesNotExist() {
        // Given
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.cancel(enrollmentId))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(eventRepository, never()).save(any());
        verify(auditPublisher, never()).publish(any());
    }

    // ── Scenario 3: Audit event content ──────────────────────────────────────

    @Test
    @DisplayName("Given a successful cancellation, " +
            "When cancel completes, " +
            "Then audit event contains correct recovered spots")
    void shouldPublishAuditEventWithCorrectRecoveredSpots() {
        // Given
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(activeEnrollment));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(enrollmentRepository.save(any())).thenReturn(activeEnrollment);
        when(eventRepository.save(any())).thenReturn(event);

        ArgumentCaptor<EnrollmentCancelledAuditEvent> captor =
                ArgumentCaptor.forClass(EnrollmentCancelledAuditEvent.class);

        // When
        service.cancel(enrollmentId);

        // Then
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().enrollmentId()).isEqualTo(enrollmentId);
        assertThat(captor.getValue().spotsRecovered()).isEqualTo(8); // 7 + 1
    }
}
