package com.example.infrastructure.persistence.adapter;

import com.example.domain.model.Participant;
import com.example.domain.port.out.ParticipantRepositoryPort;
import com.example.infrastructure.persistence.entity.ParticipantEntity;
import com.example.infrastructure.persistence.repository.SpringParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven adapter — implements {@link ParticipantRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class ParticipantJpaAdapter implements ParticipantRepositoryPort {

    private final SpringParticipantRepository springParticipantRepository;

    @Override
    public Optional<Participant> findById(UUID id) {
        return springParticipantRepository.findById(id)
                .map(ParticipantEntity::toDomain);
    }
}
