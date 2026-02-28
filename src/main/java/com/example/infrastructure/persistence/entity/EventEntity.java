package com.example.infrastructure.persistence.entity;

import com.example.domain.model.Event;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code events} table.
 *
 * <p>Isolated from the domain layer. Conversion to/from domain objects is
 * handled by {@link #toDomain()} and {@link #fromDomain(Event)}.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private int availableSpots;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    /** Optimistic locking version — prevents concurrent over-enrollment. */
    @Version
    private Long version;

    // ── Mapping ───────────────────────────────────────────────────────────────

    public Event toDomain() {
        return Event.builder()
                .id(id)
                .name(name)
                .maxCapacity(maxCapacity)
                .availableSpots(availableSpots)
                .eventDate(eventDate)
                .version(version)
                .build();
    }

    public static EventEntity fromDomain(Event event) {
        return EventEntity.builder()
                .id(event.getId())
                .name(event.getName())
                .maxCapacity(event.getMaxCapacity())
                .availableSpots(event.getAvailableSpots())
                .eventDate(event.getEventDate())
                .version(event.getVersion())
                .build();
    }
}
