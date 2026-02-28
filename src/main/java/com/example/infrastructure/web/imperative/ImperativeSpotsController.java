package com.example.infrastructure.web.imperative;

import com.example.domain.port.in.QueryAvailableSpotsPort;
import com.example.infrastructure.web.dto.response.SpotsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Driving adapter — exposes the imperative query spots use case (CU-03) via REST. */
@RestController
@RequestMapping("/api/v1/imperative")
@RequiredArgsConstructor
public class ImperativeSpotsController {

    @Qualifier("imperativeQuerySpots")
    private final QueryAvailableSpotsPort queryAvailableSpotsPort;

    @GetMapping("/events/{id}/spots")
    public SpotsResponse querySpots(@PathVariable UUID id) {
        return SpotsResponse.fromDomain(queryAvailableSpotsPort.querySpots(id));
    }
}
