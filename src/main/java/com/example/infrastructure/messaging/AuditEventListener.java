package com.example.infrastructure.messaging;

import com.example.infrastructure.messaging.event.EnrollmentCancelledAuditEvent;
import com.example.infrastructure.messaging.event.EnrollmentCreatedAuditEvent;
import com.example.infrastructure.messaging.event.SpotsQueriedAuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchronous listener that writes structured audit logs for all business events.
 *
 * <p>Logging strategy (spec.md §9.1):
 * <ul>
 *   <li>Successful operations → {@code INFO}
 *   <li>Cancellations → {@code INFO} (business-significant, not an error)
 *   <li>Errors are NOT logged here — handled exclusively by {@code GlobalExceptionHandler}
 * </ul>
 *
 * <p>Format: {@code [AUDIT] action=ACTION | field=value | ...}
 * This prefix enables log filtering without a dedicated audit framework.
 */
@Slf4j
@Component
public class AuditEventListener {

    @EventListener
    public void onEnrollmentCreated(EnrollmentCreatedAuditEvent event) {
        log.info("[AUDIT] action=ENROLLMENT_CREATED | event={} | participant={} | spots_remaining={}",
                event.eventId(), event.participantId(), event.spotsRemaining());
    }

    @EventListener
    public void onEnrollmentCancelled(EnrollmentCancelledAuditEvent event) {
        log.info("[AUDIT] action=ENROLLMENT_CANCELLED | enrollment={} | event={} | spots_recovered={}",
                event.enrollmentId(), event.eventId(), event.spotsRecovered());
    }

    @EventListener
    public void onSpotsQueried(SpotsQueriedAuditEvent event) {
        log.info("[AUDIT] action=SPOTS_QUERIED | event={} | available_spots={}",
                event.eventId(), event.availableSpots());
    }
}
