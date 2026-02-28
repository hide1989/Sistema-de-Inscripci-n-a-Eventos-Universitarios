package com.example.infrastructure.web.dto.response;

import com.example.domain.model.Participant;
import lombok.Builder;

import java.util.List;

/**
 * HTTP response body for the list enrolled participants use case (CU-04).
 * {@code total} is derived from the list size to avoid duplication.
 */
@Builder
public record ParticipantListResponse(
        List<ParticipantDto> participants,
        int total
) {

    public static ParticipantListResponse fromDomain(List<Participant> participants) {
        List<ParticipantDto> dtos = participants.stream()
                .map(ParticipantDto::fromDomain)
                .toList();
        return ParticipantListResponse.builder()
                .participants(dtos)
                .total(dtos.size())
                .build();
    }
}
