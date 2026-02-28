package com.example.domain.exception;

import java.util.UUID;

/** Thrown when an {@code Enrollment} with the given id does not exist in the repository. */
public class EnrollmentNotFoundException extends DomainException {

    public EnrollmentNotFoundException(UUID enrollmentId) {
        super("Enrollment not found: " + enrollmentId, "ENROLLMENT_NOT_FOUND");
    }
}
