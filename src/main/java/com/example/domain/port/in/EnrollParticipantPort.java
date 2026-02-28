package com.example.domain.port.in;

import com.example.domain.model.Enrollment;

import java.util.UUID;

/**
 * Driving port — Enroll Participant use case (CU-01).
 *
 * <p>Two implementations exist: {@code ImperativeEnrollmentService} and
 * {@code FunctionalEnrollmentService}. Both share this contract; the
 * difference is in their internal processing style.
 *
 * @see com.example.application.imperative.ImperativeEnrollmentService
 * @see com.example.application.functional.FunctionalEnrollmentService
 */
public interface EnrollParticipantPort {

    /**
     * Enrolls a participant in an event within a serializable transaction.
     *
     * @param eventId       UUID of the target event
     * @param participantId UUID of the participant to enroll
     * @return the persisted {@link Enrollment} with ACTIVE status
     * @throws com.example.domain.exception.EventNotFoundException       event does not exist
     * @throws com.example.domain.exception.ParticipantNotFoundException participant does not exist
     * @throws com.example.domain.exception.EventFullException           no spots available
     * @throws com.example.domain.exception.DuplicateEnrollmentException participant already enrolled
     */
    Enrollment enroll(UUID eventId, UUID participantId);
}
