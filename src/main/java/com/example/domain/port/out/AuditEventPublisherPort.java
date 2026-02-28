package com.example.domain.port.out;

/**
 * Driven port — Audit event publishing abstraction.
 *
 * <p>Decouples the application layer from the specific event mechanism.
 * The current implementation ({@code SpringAuditEventPublisher}) uses
 * synchronous Spring Application Events. Replacing it with an async broker
 * (Kafka, RabbitMQ) requires only a new adapter — no domain changes (OCP).
 */
public interface AuditEventPublisherPort {

    /**
     * Publishes an audit event to registered listeners.
     *
     * @param auditEvent a domain event record (e.g. EnrollmentCreatedAuditEvent)
     */
    void publish(Object auditEvent);
}
