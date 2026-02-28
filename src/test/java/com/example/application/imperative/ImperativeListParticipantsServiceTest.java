package com.example.application.imperative;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.model.*;
import com.example.domain.port.out.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImperativeListParticipantsService — Unit Tests")
class ImperativeListParticipantsServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private ParticipantRepositoryPort participantRepository;

    @InjectMocks
    private ImperativeListParticipantsService service;

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an event with 3 ACTIVE and 1 CANCELLED enrollment, " +
            "When listParticipants is called, " +
            "Then only the 3 ACTIVE participants are returned")
    void shouldReturnOnlyActiveParticipants() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();

        Event event = Event.builder().id(eventId).name("Test").maxCapacity(10)
                .availableSpots(7).eventDate(LocalDateTime.now()).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Only ACTIVE enrollments returned from repo (filtering is at DB level)
        List<Enrollment> activeEnrollments = List.of(
                Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                        .participantId(p1).status(EnrollmentStatus.ACTIVE).build(),
                Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                        .participantId(p2).status(EnrollmentStatus.ACTIVE).build(),
                Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                        .participantId(p3).status(EnrollmentStatus.ACTIVE).build()
        );
        when(enrollmentRepository.findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE))
                .thenReturn(activeEnrollments);

        when(participantRepository.findById(p1)).thenReturn(Optional.of(
                Participant.builder().id(p1).name("Ana").email("ana@test.co").build()));
        when(participantRepository.findById(p2)).thenReturn(Optional.of(
                Participant.builder().id(p2).name("Carlos").email("carlos@test.co").build()));
        when(participantRepository.findById(p3)).thenReturn(Optional.of(
                Participant.builder().id(p3).name("María").email("maria@test.co").build()));

        // When
        List<Participant> result = service.listParticipants(eventId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Participant::getName)
                .containsExactlyInAnyOrder("Ana", "Carlos", "María");
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an event with no active enrollments, " +
            "When listParticipants is called, " +
            "Then an empty list is returned")
    void shouldReturnEmptyListWhenNoActiveEnrollments() {
        // Given
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder().id(eventId).name("Empty Event").maxCapacity(10)
                .availableSpots(10).eventDate(LocalDateTime.now()).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(enrollmentRepository.findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());

        // When
        List<Participant> result = service.listParticipants(eventId);

        // Then
        assertThat(result).isEmpty();
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a non-existent event UUID, " +
            "When listParticipants is called, " +
            "Then EventNotFoundException is thrown")
    void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.listParticipants(eventId))
                .isInstanceOf(EventNotFoundException.class);
    }
}
