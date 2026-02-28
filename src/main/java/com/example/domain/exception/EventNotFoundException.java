package com.example.domain.exception;

import java.util.UUID;

/** Thrown when an {@code Event} with the given id does not exist in the repository. */
public class EventNotFoundException extends DomainException {

    public EventNotFoundException(UUID eventId) {
        super("Event not found: " + eventId, "EVENT_NOT_FOUND");
    }
}
