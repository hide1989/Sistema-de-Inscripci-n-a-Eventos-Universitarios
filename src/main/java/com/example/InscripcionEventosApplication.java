package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Inscripcion Eventos POC application.
 *
 * <p>Implements a hexagonal architecture (Ports &amp; Adapters) with two processing
 * styles per use case: imperative and functional (Streams-based).
 *
 * <p>Infrastructure: H2 in-memory · Spring Data JPA · Spring Boot Actuator
 */
@SpringBootApplication
public class InscripcionEventosApplication {

    public static void main(String[] args) {
        SpringApplication.run(InscripcionEventosApplication.class, args);
    }
}
