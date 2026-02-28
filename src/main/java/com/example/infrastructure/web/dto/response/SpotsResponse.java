package com.example.infrastructure.web.dto.response;

import com.example.domain.model.Event;
import lombok.Builder;

import java.util.UUID;

/**
 * HTTP response body for the query available spots use case (CU-03).
 */
@Builder
public record SpotsResponse(
        UUID eventId,
        String eventName,
        int maxCapacity,
        int availableSpots
) {

    public static SpotsResponse fromDomain(Event event) {
        return SpotsResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .maxCapacity(event.getMaxCapacity())
                .availableSpots(event.getAvailableSpots())
                .build();
    }
}
