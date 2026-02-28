package com.example.infrastructure.web.functional;

import com.example.domain.port.in.EnrollParticipantPort;
import com.example.infrastructure.web.dto.request.EnrollmentRequest;
import com.example.infrastructure.web.dto.response.EnrollmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Driving adapter — exposes the functional enroll use case (CU-01) via REST. */
@RestController
@RequestMapping("/api/v1/functional")
@RequiredArgsConstructor
public class FunctionalEnrollmentController {

    @Qualifier("functionalEnroll")
    private final EnrollParticipantPort enrollParticipantPort;

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request) {
        return EnrollmentResponse.fromDomain(
                enrollParticipantPort.enroll(request.eventId(), request.participantId()));
    }
}
