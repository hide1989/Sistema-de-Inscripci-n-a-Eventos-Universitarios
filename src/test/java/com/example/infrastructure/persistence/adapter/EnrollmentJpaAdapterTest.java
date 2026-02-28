package com.example.infrastructure.persistence.adapter;

import com.example.domain.model.Enrollment;
import com.example.domain.model.EnrollmentStatus;
import com.example.infrastructure.persistence.entity.EnrollmentEntity;
import com.example.infrastructure.persistence.repository.SpringEnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(EnrollmentJpaAdapter.class)
@DisplayName("EnrollmentJpaAdapter — Integration Tests (H2)")
class EnrollmentJpaAdapterTest {

    @Autowired private EnrollmentJpaAdapter adapter;
    @Autowired private SpringEnrollmentRepository springRepo;

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a new enrollment domain object, " +
            "When save is called, " +
            "Then it is persisted and retrievable by id")
    void shouldPersistAndRetrieveEnrollment() {
        // Given
        Enrollment enrollment = Enrollment.builder()
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .enrolledAt(LocalDateTime.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();

        // When
        Enrollment saved = adapter.save(enrollment);
        Optional<Enrollment> found = adapter.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given enrollments with mixed statuses, " +
            "When findByEventIdAndStatus(ACTIVE) is called, " +
            "Then only ACTIVE enrollments are returned")
    void shouldFilterEnrollmentsByStatus() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();

        springRepo.save(EnrollmentEntity.builder().eventId(eventId).participantId(p1)
                .enrolledAt(LocalDateTime.now()).status(EnrollmentStatus.ACTIVE).build());
        springRepo.save(EnrollmentEntity.builder().eventId(eventId).participantId(p2)
                .enrolledAt(LocalDateTime.now()).status(EnrollmentStatus.CANCELLED).build());

        // When
        List<Enrollment> active = adapter.findByEventIdAndStatus(eventId, EnrollmentStatus.ACTIVE);

        // Then
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getParticipantId()).isEqualTo(p1);
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given an active enrollment exists, " +
            "When existsByEventIdAndParticipantIdAndStatus is called, " +
            "Then true is returned")
    void shouldReturnTrueForExistingActiveEnrollment() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        springRepo.save(EnrollmentEntity.builder().eventId(eventId).participantId(participantId)
                .enrolledAt(LocalDateTime.now()).status(EnrollmentStatus.ACTIVE).build());

        // When
        boolean exists = adapter.existsByEventIdAndParticipantIdAndStatus(
                eventId, participantId, EnrollmentStatus.ACTIVE);

        // Then
        assertThat(exists).isTrue();
    }

    // ── Scenario 4 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given no enrollment for a participant, " +
            "When existsByEventIdAndParticipantIdAndStatus is called, " +
            "Then false is returned")
    void shouldReturnFalseWhenNoEnrollmentExists() {
        // Given — no enrollment saved

        // When
        boolean exists = adapter.existsByEventIdAndParticipantIdAndStatus(
                UUID.randomUUID(), UUID.randomUUID(), EnrollmentStatus.ACTIVE);

        // Then
        assertThat(exists).isFalse();
    }
}
