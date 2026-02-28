package com.example.infrastructure.persistence.adapter;

import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.domain.port.out.EnrollmentRepositoryPort;
import com.example.infrastructure.persistence.entity.EnrollmentEntity;
import com.example.infrastructure.persistence.repository.SpringEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Driven adapter — implements {@link EnrollmentRepositoryPort} using Spring Data JPA.
 *
 * <p>Translates between {@link EnrollmentEntity} (JPA) and {@link Enrollment} (domain).
 */
@Component
@RequiredArgsConstructor
public class EnrollmentJpaAdapter implements EnrollmentRepositoryPort {

    private final SpringEnrollmentRepository springEnrollmentRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentEntity entity = EnrollmentEntity.fromDomain(enrollment);
        return springEnrollmentRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Enrollment> findById(UUID id) {
        return springEnrollmentRepository.findById(id)
                .map(EnrollmentEntity::toDomain);
    }

    @Override
    public List<Enrollment> findByEventIdAndStatus(UUID eventId, EnrollmentStatus status) {
        return springEnrollmentRepository.findByEventIdAndStatus(eventId, status)
                .stream()
                .map(EnrollmentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEventIdAndParticipantIdAndStatus(UUID eventId, UUID participantId,
                                                             EnrollmentStatus status) {
        return springEnrollmentRepository
                .existsByEventIdAndParticipantIdAndStatus(eventId, participantId, status);
    }
}
