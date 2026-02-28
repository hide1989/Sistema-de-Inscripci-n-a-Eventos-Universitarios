package com.example.infrastructure.web.functional;

import com.example.domain.port.in.ListEnrolledParticipantsPort;
import com.example.infrastructure.web.dto.response.ParticipantListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Driving adapter — exposes the functional list participants use case (CU-04) via REST. */
@RestController
@RequestMapping("/api/v1/functional")
@RequiredArgsConstructor
public class FunctionalParticipantsController {

    @Qualifier("functionalListParticipants")
    private final ListEnrolledParticipantsPort listEnrolledParticipantsPort;

    @GetMapping("/events/{id}/participants")
    public ParticipantListResponse listParticipants(@PathVariable UUID id) {
        return ParticipantListResponse.fromDomain(listEnrolledParticipantsPort.listParticipants(id));
    }
}
