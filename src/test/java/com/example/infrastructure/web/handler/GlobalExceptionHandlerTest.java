package com.example.infrastructure.web.handler;

import com.example.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} to manually wire the handler
 * with a minimal test controller — avoids Spring Boot auto-configuration
 * interfering with exception handler resolution in {@code @WebMvcTest} contexts.
 */
@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given EventNotFoundException, " +
            "When handler processes it, " +
            "Then HTTP 404 with code EVENT_NOT_FOUND is returned")
    void shouldReturn404ForEventNotFoundException() throws Exception {
        mockMvc.perform(get("/test/event-not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/event-not-found"));
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given EventFullException, " +
            "When handler processes it, " +
            "Then HTTP 409 with code EVENT_FULL is returned")
    void shouldReturn409ForEventFullException() throws Exception {
        mockMvc.perform(get("/test/event-full").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_FULL"));
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given DuplicateEnrollmentException, " +
            "When handler processes it, " +
            "Then HTTP 409 with code DUPLICATE_ENROLLMENT is returned")
    void shouldReturn409ForDuplicateEnrollmentException() throws Exception {
        mockMvc.perform(get("/test/duplicate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ENROLLMENT"));
    }

    // ── Scenario 4 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given OptimisticLockingFailureException, " +
            "When handler processes it, " +
            "Then HTTP 409 with code OPTIMISTIC_LOCK_CONFLICT is returned")
    void shouldReturn409ForOptimisticLockConflict() throws Exception {
        mockMvc.perform(get("/test/optimistic-lock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    // ── Scenario 5 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given EnrollmentNotFoundException, " +
            "When handler processes it, " +
            "Then HTTP 404 with code ENROLLMENT_NOT_FOUND is returned")
    void shouldReturn404ForEnrollmentNotFoundException() throws Exception {
        mockMvc.perform(get("/test/enrollment-not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));
    }

    // ── Scenario 6 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Given a malformed UUID in request body, " +
            "When Jackson cannot deserialize it, " +
            "Then HTTP 400 with code MALFORMED_REQUEST is returned (not 500)")
    void shouldReturn400ForMalformedUUID() throws Exception {
        // First segment has 7 chars ("542cb20") — valid UUID needs 8 ("542cb200")
        String badBody = "{\"eventId\": \"542cb20-d3cc-4ee7-8089-6ccd24cbc1d0\"}";

        mockMvc.perform(post("/test/with-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // ── Minimal test controller ───────────────────────────────────────────────

    @RestController
    @RequestMapping("/test")
    static class TestController {

        record UuidBody(UUID eventId) {}

        @GetMapping("/event-not-found")
        public void eventNotFound() {
            throw new EventNotFoundException(UUID.randomUUID());
        }

        @GetMapping("/event-full")
        public void eventFull() {
            throw new EventFullException(UUID.randomUUID());
        }

        @GetMapping("/duplicate")
        public void duplicate() {
            throw new DuplicateEnrollmentException(UUID.randomUUID(), UUID.randomUUID());
        }

        @GetMapping("/optimistic-lock")
        public void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException("EnrollmentEntity", UUID.randomUUID());
        }

        @GetMapping("/enrollment-not-found")
        public void enrollmentNotFound() {
            throw new EnrollmentNotFoundException(UUID.randomUUID());
        }

        @PostMapping("/with-body")
        public void withBody(@RequestBody UuidBody body) {
            // body is only used to trigger Jackson deserialization of UUID fields
        }
    }
}
