package com.example.infrastructure.web.imperative;

import com.example.domain.port.in.EnrollParticipantPort;
import com.example.infrastructure.web.dto.request.EnrollmentRequest;
import com.example.infrastructure.web.dto.response.EnrollmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Driving adapter — exposes the imperative enroll use case (CU-01) via REST.
 *
 * <p>No business logic here. The controller's sole responsibility (SRP) is to
 * translate HTTP concerns into domain port calls and map the result back to HTTP.
 */
@RestController
@RequestMapping("/api/v1/imperative")
@RequiredArgsConstructor
public class ImperativeEnrollmentController {

    @Qualifier("imperativeEnroll")
    private final EnrollParticipantPort enrollParticipantPort;

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request) {
        return EnrollmentResponse.fromDomain(
                enrollParticipantPort.enroll(request.eventId(), request.participantId()));
    }
}
