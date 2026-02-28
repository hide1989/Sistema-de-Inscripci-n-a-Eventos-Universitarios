package com.example.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core domain entity representing a university event with a limited capacity.
 *
 * <p>This class belongs exclusively to the domain layer and carries no
 * framework dependencies. {@code availableSpots} is the concurrency-sensitive
 * field protected by optimistic locking at the JPA adapter level.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private UUID id;

    /** Human-readable name of the event. */
    private String name;

    /** Total seats offered for this event. Immutable after creation. */
    private int maxCapacity;

    /**
     * Remaining seats. Decremented on enroll, incremented on cancellation.
     * Must always satisfy {@code 0 <= availableSpots <= maxCapacity}.
     */
    private int availableSpots;

    private LocalDateTime eventDate;

    /**
     * Optimistic lock version. Mapped from {@code EventEntity.version}.
     * Never modified by domain logic; managed by the persistence adapter.
     */
    private Long version;
}
