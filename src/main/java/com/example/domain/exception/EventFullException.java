package com.example.domain.exception;

import java.util.UUID;

/** Thrown when an enrollment is attempted on an event with no available spots. */
public class EventFullException extends DomainException {

    public EventFullException(UUID eventId) {
        super("Event has no available spots: " + eventId, "EVENT_FULL");
    }
}
