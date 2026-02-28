package com.example.domain.port.out;

import com.example.domain.model.Event;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven port — Event persistence abstraction.
 *
 * <p>Implemented by {@code EventJpaAdapter} in the infrastructure layer.
 * The domain depends only on this interface, never on JPA classes directly (DIP).
 */
public interface EventRepositoryPort {

    Optional<Event> findById(UUID id);

    Event save(Event event);
}
