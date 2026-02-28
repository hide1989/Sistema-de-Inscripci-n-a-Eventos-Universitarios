package com.example.infrastructure.persistence.repository;

import com.example.infrastructure.persistence.entity.ParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data JPA repository for {@link ParticipantEntity}. Internal to the persistence adapter. */
public interface SpringParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {
}
