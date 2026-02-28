package com.example.domain.port.in;

import com.example.domain.model.Event;

import java.util.UUID;

/**
 * Driving port — Query Available Spots use case (CU-03).
 *
 * <p>Read-only operation; no transaction required.
 */
public interface QueryAvailableSpotsPort {

    /**
     * Returns the event with its current spot availability.
     *
     * @param eventId UUID of the event to query
     * @return the {@link Event} domain object reflecting current {@code availableSpots}
     * @throws com.example.domain.exception.EventNotFoundException event does not exist
     */
    Event querySpots(UUID eventId);
}
