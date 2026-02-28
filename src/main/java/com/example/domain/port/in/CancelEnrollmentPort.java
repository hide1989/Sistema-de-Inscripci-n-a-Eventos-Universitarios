package com.example.domain.port.in;

import java.util.UUID;

/**
 * Driving port — Cancel Enrollment use case (CU-02).
 *
 * <p>Cancels an active enrollment and restores the event's available spots
 * within a single transaction.
 */
public interface CancelEnrollmentPort {

    /**
     * Cancels the enrollment identified by {@code enrollmentId}.
     *
     * @param enrollmentId UUID of the enrollment to cancel
     * @throws com.example.domain.exception.EnrollmentNotFoundException enrollment does not exist
     */
    void cancel(UUID enrollmentId);
}
