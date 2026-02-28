package com.example.domain.model;

import lombok.*;

import java.util.UUID;

/**
 * Core domain entity representing a person that can enroll in events.
 *
 * <p>The {@code email} field is unique across all participants and serves
 * as the natural business identifier.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Participant {

    private UUID id;
    private String name;

    /** Unique institutional email address. */
    private String email;
}
