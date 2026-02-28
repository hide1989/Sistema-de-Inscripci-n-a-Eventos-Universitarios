package com.example.application.imperative;

import com.example.domain.exception.EventNotFoundException;
import com.example.domain.exception.ParticipantNotFoundException;
import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.model.Participant;
import com.example.domain.port.in.ListEnrolledParticipantsPort;
import com.example.domain.port.out.EnrollmentRepositoryPort;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.domain.port.out.ParticipantRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Imperative implementation of {@link ListEnrolledParticipantsPort} (CU-04).
 *
 * <p>Uses an explicit {@code for} loop and an {@code ArrayList} to build the
 * result. Compare with
 * {@link com.example.application.functional.FunctionalListParticipantsService}
 * which uses a Stream pipeline.
 */
@RequiredArgsConstructor
public class ImperativeListParticipantsService implements ListEnrolledParticipantsPort {

    private final EventRepositoryPort eventRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final ParticipantRepositoryPort participantRepository;

    @Override
    public List<Participant> listParticipants(UUID eventId) {

        // 1. Validate event existence
        eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // 2. Retrieve ACTIVE enrollments
        List<Enrollment> activeEnrollments =
                enrollmentRepository.findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE);

        // 3. Build participant list — explicit loop (imperative style)
        List<Participant> participants = new ArrayList<>();
        for (Enrollment enrollment : activeEnrollments) {
            Participant participant = participantRepository
                    .findById(enrollment.getParticipantId())
                    .orElseThrow(() -> new ParticipantNotFoundException(enrollment.getParticipantId()));
            participants.add(participant);
        }

        return participants;
    }
}
