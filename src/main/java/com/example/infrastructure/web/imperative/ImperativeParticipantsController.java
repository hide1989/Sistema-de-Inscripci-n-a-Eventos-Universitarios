package com.example.infrastructure.web.imperative;

import com.example.domain.port.in.ListEnrolledParticipantsPort;
import com.example.infrastructure.web.dto.response.ParticipantListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Driving adapter — exposes the imperative list participants use case (CU-04) via REST. */
@RestController
@RequestMapping("/api/v1/imperative")
@RequiredArgsConstructor
public class ImperativeParticipantsController {

    @Qualifier("imperativeListParticipants")
    private final ListEnrolledParticipantsPort listEnrolledParticipantsPort;

    @GetMapping("/events/{id}/participants")
    public ParticipantListResponse listParticipants(@PathVariable UUID id) {
        return ParticipantListResponse.fromDomain(listEnrolledParticipantsPort.listParticipants(id));
    }
}
