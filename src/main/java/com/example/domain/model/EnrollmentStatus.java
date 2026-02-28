package com.example.domain.model;

/** Represents the lifecycle state of an {@link Enrollment}. */
public enum EnrollmentStatus {
    /** The participant is actively registered in the event. */
    ACTIVE,
    /** The participant has cancelled their registration. */
    CANCELLED
}
