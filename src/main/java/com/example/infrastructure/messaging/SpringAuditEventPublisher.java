package com.example.infrastructure.messaging;

import com.example.domain.port.out.AuditEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Driven adapter — implements {@link AuditEventPublisherPort} using Spring Application Events.
 *
 * <p>Events are published synchronously (no {@code @Async}), ensuring that audit
 * records are written within the same thread as the business operation.
 *
 * <p>To replace with an async broker (Kafka, RabbitMQ), create a new adapter
 * implementing {@link AuditEventPublisherPort} and swap the bean — no domain
 * changes required (OCP).
 */
@Component
@RequiredArgsConstructor
public class SpringAuditEventPublisher implements AuditEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object auditEvent) {
        applicationEventPublisher.publishEvent(auditEvent);
    }
}
