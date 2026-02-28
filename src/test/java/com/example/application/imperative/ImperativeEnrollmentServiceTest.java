package com.example.application.imperative;

import com.example.domain.exception.DuplicateEnrollmentException;
import com.example.domain.exception.EventFullException;
import com.example.domain.exception.EventNotFoundException;
import com.example.domain.exception.ParticipantNotFoundException;
import com.example.domain.model.*;
import com.example.domain.port.out.*;
import com.example.infrastructure.messaging.event.EnrollmentCreatedAuditEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImperativeEnrollmentService — Unit Tests")
class ImperativeEnrollmentServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private ParticipantRepositoryPort participantRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private AuditEventPublisherPort auditPublisher;

    @InjectMocks
    private ImperativeEnrollmentService service;

    private UUID eventId;
    private UUID participantId;
    private Event eventWithSpots;
    private Participant participant;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        participantId = UUID.randomUUID();

        eventWithSpots = Event.builder()
                .id(eventId)
                .name("Test Event")
                .maxCapacity(10)
                .availableSpots(5)
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();

        participant = Participant.builder()
                .id(participantId)
                .name("Ana García")
                .email("ana@test.edu.co")
                .build();
    }

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an event with available spots and a non-enrolled participant, " +
            "When enroll is called, " +
            "Then the enrollment is created with ACTIVE status and spots are decremented")
    void shouldCreateEnrollmentWhenEventHasSpotsAndParticipantIsNew() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpots));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)).thenReturn(false);

        Enrollment savedEnrollment = Enrollment.builder()
                .id(UUID.randomUUID()).eventId(eventId).participantId(participantId)
                .enrolledAt(LocalDateTime.now()).status(EnrollmentStatus.ACTIVE).build();
        when(enrollmentRepository.save(any())).thenReturn(savedEnrollment);
        when(eventRepository.save(any())).thenReturn(eventWithSpots);

        // When
        Enrollment result = service.enroll(eventId, participantId);

        // Then
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(eventRepository).save(argThat(e -> e.getAvailableSpots() == 4));
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(auditPublisher).publish(any(EnrollmentCreatedAuditEvent.class));
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an event with 0 available spots, " +
            "When enroll is called, " +
            "Then EventFullException is thrown and no enrollment is persisted")
    void shouldThrowEventFullExceptionWhenNoSpotsAvailable() {
        // Given
        Event fullEvent = Event.builder().id(eventId).name("Full Event")
                .maxCapacity(10).availableSpots(0).eventDate(LocalDateTime.now().plusDays(1)).build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(fullEvent));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(EventFullException.class)
                .hasMessageContaining(eventId.toString());

        verify(enrollmentRepository, never()).save(any());
        verify(auditPublisher, never()).publish(any());
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a participant already enrolled in the event, " +
            "When enroll is called again, " +
            "Then DuplicateEnrollmentException is thrown")
    void shouldThrowDuplicateEnrollmentExceptionWhenAlreadyEnrolled() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpots));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(DuplicateEnrollmentException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    // ── Scenario 4 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a non-existent event UUID, " +
            "When enroll is called, " +
            "Then EventNotFoundException is thrown")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(EventNotFoundException.class);
    }

    // ── Scenario 5 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a non-existent participant UUID, " +
            "When enroll is called, " +
            "Then ParticipantNotFoundException is thrown")
    void shouldThrowParticipantNotFoundExceptionWhenParticipantDoesNotExist() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpots));
        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(ParticipantNotFoundException.class);
    }

    // ── Scenario 6: Audit event content ──────────────────────────────────────

    @Test
    @DisplayName("Given a successful enrollment, " +
            "When enroll completes, " +
            "Then an audit event is published with correct remaining spots")
    void shouldPublishAuditEventWithCorrectSpotsRemaining() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpots));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(any(), any(), any()))
                .thenReturn(false);
        when(enrollmentRepository.save(any())).thenReturn(
                Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                        .participantId(participantId).status(EnrollmentStatus.ACTIVE)
                        .enrolledAt(LocalDateTime.now()).build());
        when(eventRepository.save(any())).thenReturn(eventWithSpots);

        ArgumentCaptor<EnrollmentCreatedAuditEvent> captor =
                ArgumentCaptor.forClass(EnrollmentCreatedAuditEvent.class);

        // When
        service.enroll(eventId, participantId);

        // Then
        verify(auditPublisher).publish(captor.capture());
        assertThat(captor.getValue().spotsRemaining()).isEqualTo(4); // 5 - 1
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().participantId()).isEqualTo(participantId);
    }
}
