package com.example.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit event published after a successful enrollment (CU-01).
 * Immutable record — captured by {@code AuditEventListener}.
 */
public record EnrollmentCreatedAuditEvent(
        UUID eventId,
        UUID participantId,
        int spotsRemaining,
        LocalDateTime timestamp
) {
}
