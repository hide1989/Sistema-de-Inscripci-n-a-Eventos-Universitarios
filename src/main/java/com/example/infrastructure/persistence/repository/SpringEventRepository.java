package com.example.infrastructure.persistence.repository;

import com.example.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data JPA repository for {@link EventEntity}. Internal to the persistence adapter. */
public interface SpringEventRepository extends JpaRepository<EventEntity, UUID> {
}
