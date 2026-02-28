package com.example.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit event published after a successful cancellation (CU-02).
 * Immutable record — captured by {@code AuditEventListener}.
 */
public record EnrollmentCancelledAuditEvent(
        UUID enrollmentId,
        UUID eventId,
        int spotsRecovered,
        LocalDateTime timestamp
) {
}
