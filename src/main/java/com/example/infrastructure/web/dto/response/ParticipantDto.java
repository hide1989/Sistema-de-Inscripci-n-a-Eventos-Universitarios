package com.example.infrastructure.web.dto.response;

import com.example.domain.model.Participant;
import lombok.Builder;

import java.util.UUID;

/** DTO representing a single participant in the list enrolled response (CU-04). */
@Builder
public record ParticipantDto(
        UUID participantId,
        String name,
        String email
) {

    public static ParticipantDto fromDomain(Participant participant) {
        return ParticipantDto.builder()
                .participantId(participant.getId())
                .name(participant.getName())
                .email(participant.getEmail())
                .build();
    }
}
