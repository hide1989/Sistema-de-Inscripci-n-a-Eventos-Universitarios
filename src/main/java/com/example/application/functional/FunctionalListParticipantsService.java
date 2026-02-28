package com.example.application.functional;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.exception.ParticipantNotFoundException;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.model.Participant;
import com.example.domain.port.in.ListEnrolledParticipantsPort;
import com.example.domain.port.out.EnrollmentRepositoryPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.domain.port.out.ParticipantRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Functional implementation of {@link ListEnrolledParticipantsPort} (CU-04).
 *
 * <p>Uses a Stream pipeline to project ACTIVE enrollments into participant
 * domain objects. No explicit loops or accumulator variables.
 *
 * <p>Compare with
 * {@link com.example.application.imperative.ImperativeListParticipantsService}
 * which builds the same list using a {@code for} loop and {@code ArrayList.add()}.
 */
@RequiredArgsConstructor
public class FunctionalListParticipantsService implements ListEnrolledParticipantsPort {

    private final EventRepositoryPort eventRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final ParticipantRepositoryPort participantRepository;

    @Override
    public List<Participant> listParticipants(UUID eventId) {

        // 1. Guard — event must exist
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. Stream pipeline: enrollments → participants (functional style)
        return enrollmentRepository
                .findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(enrollment -> participantRepository
                        .findById(enrollment.getParticipantId())
                        .orElseThrow(() -> new ParticipantNotFoundException(enrollment.getParticipantId())))
                .toList();
    }
}
