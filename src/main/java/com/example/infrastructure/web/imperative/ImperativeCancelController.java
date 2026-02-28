package com.example.infrastructure.web.imperative;

import com.example.domain.port.in.CancelEnrollmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Driving adapter — exposes the imperative cancel enrollment use case (CU-02) via REST. */
@RestController
@RequestMapping("/api/v1/imperative")
@RequiredArgsConstructor
public class ImperativeCancelController {

    @Qualifier("imperativeCancel")
    private final CancelEnrollmentPort cancelEnrollmentPort;

    @DeleteMapping("/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        cancelEnrollmentPort.cancel(id);
    }
}
