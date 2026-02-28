package com.example.infrastructure.persistence.adapter;

import com.example.domain.model.Event;
import com.example.domain.port.out.EventRepositoryPort;
import com.example.infrastructure.persistence.entity.EventEntity;
import com.example.infrastructure.persistence.repository.SpringEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven adapter — implements {@link EventRepositoryPort} using Spring Data JPA.
 *
 * <p>This class is the only place that knows about JPA details for Event persistence.
 * The domain layer sees only the port interface (DIP).
 */
@Component
@RequiredArgsConstructor
public class EventJpaAdapter implements EventRepositoryPort {

    private final SpringEventRepository springEventRepository;

    @Override
    public Optional<Event> findById(UUID id) {
        return springEventRepository.findById(id)
                .map(EventEntity::toDomain);
    }

    @Override
    public Event save(Event event) {
        EventEntity entity = EventEntity.fromDomain(event);
        return springEventRepository.save(entity).toDomain();
    }
}
