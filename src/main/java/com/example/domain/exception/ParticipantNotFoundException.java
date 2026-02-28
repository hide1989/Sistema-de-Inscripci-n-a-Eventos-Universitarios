package com.example.domain.exception;

import java.util.UUID;

/** Thrown when a {@code Participant} with the given id does not exist in the repository. */
public class ParticipantNotFoundException extends DomainException {

    public ParticipantNotFoundException(UUID participantId) {
        super("Participant not found: " + participantId, "PARTICIPANT_NOT_FOUND");
    }
}
