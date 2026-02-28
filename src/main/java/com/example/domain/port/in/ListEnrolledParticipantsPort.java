package com.example.domain.port.in;

import com.example.domain.model.Participant;

import java.util.List;
import java.util.UUID;

/**
 * Driving port — List Enrolled Participants use case (CU-04).
 *
 * <p>Returns only participants with an ACTIVE enrollment status.
 * The imperative implementation uses explicit loops; the functional
 * implementation uses Stream pipelines.
 */
public interface ListEnrolledParticipantsPort {

    /**
     * Lists all participants actively enrolled in the given event.
     *
     * @param eventId UUID of the event
     * @return list of {@link Participant} domain objects (may be empty, never null)
     * @throws com.example.domain.exception.EventNotFoundException event does not exist
     */
    List<Participant> listParticipants(UUID eventId);
}
