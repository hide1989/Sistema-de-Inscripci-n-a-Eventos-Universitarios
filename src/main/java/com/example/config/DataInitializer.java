package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Logs the fixed seed UUIDs on startup so they can be used directly in
 * curl/Postman requests.
 *
 * <p>The actual INSERT statements are in {@code src/main/resources/import.sql},
 * executed by Hibernate right after schema creation. This keeps UUIDs stable
 * across every restart of the in-memory H2 database.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    // ── Fixed seed UUIDs — stable across every restart ────────────────────────
    public static final String EVENT_TALLER_ID     = "aaaaaaaa-0001-0001-0001-000000000001";
    public static final String EVENT_CHARLA_ID     = "aaaaaaaa-0002-0002-0002-000000000002";
    public static final String PARTICIPANT_ANA_ID   = "bbbbbbbb-0001-0001-0001-000000000001";
    public static final String PARTICIPANT_CARLOS_ID = "bbbbbbbb-0002-0002-0002-000000000002";
    public static final String PARTICIPANT_MARIA_ID  = "bbbbbbbb-0003-0003-0003-000000000003";

    @Override
    public void run(String... args) {
        log.info("=== SEED DATA READY ===");
        log.info("EVENT   | Taller Concurrente          | id={}", EVENT_TALLER_ID);
        log.info("EVENT   | Charla Hexagonal (10 cupos) | id={}", EVENT_CHARLA_ID);
        log.info("PARTICIPANT | Ana García     | id={}", PARTICIPANT_ANA_ID);
        log.info("PARTICIPANT | Carlos López   | id={}", PARTICIPANT_CARLOS_ID);
        log.info("PARTICIPANT | María Torres   | id={}", PARTICIPANT_MARIA_ID);
        log.info("=== API base: http://localhost:8080/api/v1/{{imperative|functional}} ===");
    }
}
