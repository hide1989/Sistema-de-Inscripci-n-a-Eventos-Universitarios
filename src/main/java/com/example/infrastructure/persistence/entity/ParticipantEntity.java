package com.example.infrastructure.persistence.entity;

import com.example.domain.model.Participant;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * JPA entity for the {@code participants} table.
 *
 * <p>Email is enforced as unique at the database level.
 */
@Entity
@Table(name = "participants")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // ── Mapping ───────────────────────────────────────────────────────────────

    public Participant toDomain() {
        return Participant.builder()
                .id(id)
                .name(name)
                .email(email)
                .build();
    }

    public static ParticipantEntity fromDomain(Participant participant) {
        return ParticipantEntity.builder()
                .id(participant.getId())
                .name(participant.getName())
                .email(participant.getEmail())
                .build();
    }
}
