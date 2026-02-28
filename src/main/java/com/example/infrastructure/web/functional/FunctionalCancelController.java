package com.example.infrastructure.web.functional;

import com.example.domain.port.in.CancelEnrollmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Driving adapter — exposes the functional cancel enrollment use case (CU-02) via REST. */
@RestController
@RequestMapping("/api/v1/functional")
@RequiredArgsConstructor
public class FunctionalCancelController {

    @Qualifier("functionalCancel")
    private final CancelEnrollmentPort cancelEnrollmentPort;

    @DeleteMapping("/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        cancelEnrollmentPort.cancel(id);
    }
}
