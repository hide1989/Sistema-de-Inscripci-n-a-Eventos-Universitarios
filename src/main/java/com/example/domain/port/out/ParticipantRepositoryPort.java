package com.example.domain.port.out;

import com.example.domain.model.Participant;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven port — Participant persistence abstraction.
 *
 * <p>Implemented by {@code ParticipantJpaAdapter} in the infrastructure layer.
 */
public interface ParticipantRepositoryPort {

    Optional<Participant> findById(UUID id);
}
