package com.example.application.functional;

import com.example.domain.exception.*;
import com.example.domain.model.*;
import com.example.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Functional service tests — identical business scenarios to the imperative tests,
 * verifying that both implementations honour the same contract (LSP).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FunctionalEnrollmentService — Unit Tests (same contract as imperative)")
class FunctionalEnrollmentServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private ParticipantRepositoryPort participantRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private AuditEventPublisherPort auditPublisher;

    @InjectMocks
    private FunctionalEnrollmentService service;

    private UUID eventId;
    private UUID participantId;
    private Event eventWithSpots;
    private Participant participant;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        participantId = UUID.randomUUID();

        eventWithSpots = Event.builder()
                .id(eventId).name("Test Event").maxCapacity(10).availableSpots(5)
                .eventDate(LocalDateTime.now().plusDays(1)).build();

        participant = Participant.builder()
                .id(participantId).name("Ana García").email("ana@test.edu.co").build();
    }

    @Test
    @DisplayName("Given an event with available spots and a non-enrolled participant, " +
            "When enroll is called (functional), " +
            "Then the enrollment is created with ACTIVE status")
    void shouldCreateEnrollmentWhenEventHasSpotsAndParticipantIsNew() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithSpots));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenReturn(
                Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                        .participantId(participantId).status(EnrollmentStatus.ACTIVE)
                        .enrolledAt(LocalDateTime.now()).build());
        when(eventRepository.save(any())).thenReturn(eventWithSpots);

        // When
        Enrollment result = service.enroll(eventId, participantId);

        // Then
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(auditPublisher).publish(any());
    }

    @Test
    @DisplayName("Given an event with 0 available spots, " +
            "When enroll is called (functional), " +
            "Then EventFullException is thrown")
    void shouldThrowEventFullExceptionWhenNoSpotsAvailable() {
        // Given
        Event fullEvent = Event.builder().id(eventId).name("Full").maxCapacity(10)
                .availableSpots(0).eventDate(LocalDateTime.now().plusDays(1)).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(fullEvent));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(enrollmentRepository.existsByEventIdAndParticipantIdAndStatus(any(), any(), any()))
                .thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(EventFullException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a participant already enrolled, " +
            "When enroll is called (functional), " +
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
    }

    @Test
    @DisplayName("Given a non-existent event, " +
            "When enroll is called (functional), " +
            "Then EventNotFoundException is thrown")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        // Given
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.enroll(eventId, participantId))
                .isInstanceOf(EventNotFoundException.class);
    }
}
