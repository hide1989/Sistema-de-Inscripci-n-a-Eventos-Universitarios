package com.example.domain.exception;

import java.util.UUID;

/** Thrown when a participant tries to enroll in an event they are already registered for. */
public class DuplicateEnrollmentException extends DomainException {

    public DuplicateEnrollmentException(UUID eventId, UUID participantId) {
        super("Participant " + participantId + " is already enrolled in event " + eventId,
                "DUPLICATE_ENROLLMENT");
    }
}
