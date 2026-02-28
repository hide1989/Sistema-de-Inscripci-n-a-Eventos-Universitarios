package com.example.application.functional;

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
@DisplayName("FunctionalListParticipantsService — Unit Tests")
class FunctionalListParticipantsServiceTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private ParticipantRepositoryPort participantRepository;

    @InjectMocks
    private FunctionalListParticipantsService service;

    @Test
    @DisplayName("Given an event with 2 ACTIVE enrollments, " +
            "When listParticipants is called (functional), " +
            "Then 2 participants are returned via Stream pipeline")
    void shouldReturnParticipantsForActiveEnrollmentsUsingStreamPipeline() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(
                Event.builder().id(eventId).name("Test").maxCapacity(10)
                        .availableSpots(8).eventDate(LocalDateTime.now()).build()));

        when(enrollmentRepository.findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(
                        Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                                .participantId(p1).status(EnrollmentStatus.ACTIVE).build(),
                        Enrollment.builder().id(UUID.randomUUID()).eventId(eventId)
                                .participantId(p2).status(EnrollmentStatus.ACTIVE).build()
                ));

        when(participantRepository.findById(p1)).thenReturn(Optional.of(
                Participant.builder().id(p1).name("Ana").email("ana@test.co").build()));
        when(participantRepository.findById(p2)).thenReturn(Optional.of(
                Participant.builder().id(p2).name("Carlos").email("carlos@test.co").build()));

        // When
        List<Participant> result = service.listParticipants(eventId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Participant::getName)
                .containsExactlyInAnyOrder("Ana", "Carlos");
    }

    @Test
    @DisplayName("Given a non-existent event UUID, " +
            "When listParticipants is called (functional), " +
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
