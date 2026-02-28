package com.example.config;

import com.example.application.functional.*;
import com.example.application.imperative.*;
import com.example.domain.port.in.*;
import com.example.domain.port.out.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the hexagonal architecture: ports (interfaces) ↔ service implementations.
 *
 * <p>Explicit bean registration with {@code @Qualifier} is used because multiple
 * implementations of the same port interface exist (imperative and functional).
 * Spring would throw {@code NoUniqueBeanDefinitionException} without qualification.
 *
 * <p>Each controller injects the correct implementation via {@code @Qualifier}.
 * Swapping an implementation requires changing only this class (OCP for configuration).
 */
@Configuration
public class BeanConfiguration {

    // ── CU-01: Enroll ─────────────────────────────────────────────────────────

    @Bean
    @Qualifier("imperativeEnroll")
    public EnrollParticipantPort imperativeEnrollmentService(
            EventRepositoryPort eventRepo,
            ParticipantRepositoryPort participantRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            AuditEventPublisherPort auditPublisher) {
        return new ImperativeEnrollmentService(eventRepo, participantRepo, enrollmentRepo, auditPublisher);
    }

    @Bean
    @Qualifier("functionalEnroll")
    public EnrollParticipantPort functionalEnrollmentService(
            EventRepositoryPort eventRepo,
            ParticipantRepositoryPort participantRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            AuditEventPublisherPort auditPublisher) {
        return new FunctionalEnrollmentService(eventRepo, participantRepo, enrollmentRepo, auditPublisher);
    }

    // ── CU-02: Cancel ─────────────────────────────────────────────────────────

    @Bean
    @Qualifier("imperativeCancel")
    public CancelEnrollmentPort imperativeCancelService(
            EventRepositoryPort eventRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            AuditEventPublisherPort auditPublisher) {
        return new ImperativeCancelEnrollmentService(eventRepo, enrollmentRepo, auditPublisher);
    }

    @Bean
    @Qualifier("functionalCancel")
    public CancelEnrollmentPort functionalCancelService(
            EventRepositoryPort eventRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            AuditEventPublisherPort auditPublisher) {
        return new FunctionalCancelEnrollmentService(eventRepo, enrollmentRepo, auditPublisher);
    }

    // ── CU-03: Query Spots ────────────────────────────────────────────────────

    @Bean
    @Qualifier("imperativeQuerySpots")
    public QueryAvailableSpotsPort imperativeQuerySpotsService(
            EventRepositoryPort eventRepo,
            AuditEventPublisherPort auditPublisher) {
        return new ImperativeQuerySpotsService(eventRepo, auditPublisher);
    }

    @Bean
    @Qualifier("functionalQuerySpots")
    public QueryAvailableSpotsPort functionalQuerySpotsService(
            EventRepositoryPort eventRepo,
            AuditEventPublisherPort auditPublisher) {
        return new FunctionalQuerySpotsService(eventRepo, auditPublisher);
    }

    // ── CU-04: List Participants ──────────────────────────────────────────────

    @Bean
    @Qualifier("imperativeListParticipants")
    public ListEnrolledParticipantsPort imperativeListParticipantsService(
            EventRepositoryPort eventRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            ParticipantRepositoryPort participantRepo) {
        return new ImperativeListParticipantsService(eventRepo, enrollmentRepo, participantRepo);
    }

    @Bean
    @Qualifier("functionalListParticipants")
    public ListEnrolledParticipantsPort functionalListParticipantsService(
            EventRepositoryPort eventRepo,
            EnrollmentRepositoryPort enrollmentRepo,
            ParticipantRepositoryPort participantRepo) {
        return new FunctionalListParticipantsService(eventRepo, enrollmentRepo, participantRepo);
    }
}
