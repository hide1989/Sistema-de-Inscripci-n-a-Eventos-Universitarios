package com.example.infrastructure.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit event published after a spots query (CU-03).
 * Immutable record — captured by {@code AuditEventListener}.
 */
public record SpotsQueriedAuditEvent(
        UUID eventId,
        int availableSpots,
        LocalDateTime timestamp
) {
}
