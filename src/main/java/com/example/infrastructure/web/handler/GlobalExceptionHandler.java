package com.example.infrastructure.web.handler;

import com.example.domain.exception.*;
import com.example.infrastructure.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised HTTP error mapping for all domain and infrastructure exceptions (SRP).
 *
 * <p>This is the <b>only</b> place in the application that:
 * <ul>
 *   <li>Logs errors related to failed requests.
 *   <li>Converts exceptions to {@link ErrorResponse} with the correct HTTP status.
 * </ul>
 *
 * <p>Application services throw typed domain exceptions without any HTTP awareness.
 * Each exception type maps to a dedicated handler method for unambiguous resolution
 * and consistent {@code @ResponseStatus} usage (avoids mixing ResponseEntity with
 * @ResponseStatus which can cause ordering issues in Spring MVC 6.x).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEventNotFound(EventNotFoundException ex, HttpServletRequest request) {
        log.warn("[ERROR] code={} | message={} | path={}", ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(EnrollmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEnrollmentNotFound(EnrollmentNotFoundException ex,
                                                   HttpServletRequest request) {
        log.warn("[ERROR] code={} | message={} | path={}", ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ParticipantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleParticipantNotFound(ParticipantNotFoundException ex,
                                                    HttpServletRequest request) {
        log.warn("[ERROR] code={} | message={} | path={}", ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────

    @ExceptionHandler(EventFullException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEventFull(EventFullException ex, HttpServletRequest request) {
        log.warn("[ERROR] code={} | message={} | path={}", ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateEnrollmentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEnrollment(DuplicateEnrollmentException ex,
                                                    HttpServletRequest request) {
        log.warn("[ERROR] code={} | message={} | path={}", ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles optimistic locking conflicts triggered by concurrent enrollment attempts.
     * Returns 409 Conflict with code {@code OPTIMISTIC_LOCK_CONFLICT}.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                               HttpServletRequest request) {
        log.error("[ERROR] action=OPTIMISTIC_LOCK_CONFLICT | path={} | detail={}",
                request.getRequestURI(), ex.getMessage());
        return ErrorResponse.of(
                "OPTIMISTIC_LOCK_CONFLICT",
                "Concurrent modification detected. Please retry the operation.",
                request.getRequestURI());
    }

    // ── 400 Bad Request ───────────────────────────────────────────────────────

    /**
     * Handles bean validation failures ({@code @Valid} on request bodies).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex,
                                           HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");

        log.warn("[ERROR] action=VALIDATION_ERROR | path={} | detail={}", request.getRequestURI(), detail);
        return ErrorResponse.of("VALIDATION_ERROR", detail, request.getRequestURI());
    }

    /**
     * Handles malformed JSON payloads and type-conversion failures (e.g. an invalid
     * UUID string that Jackson cannot deserialize into {@code java.util.UUID}).
     *
     * <p>Without this handler, Jackson's {@link HttpMessageNotReadableException} would
     * bubble up to the generic {@code Exception} catch-all and be returned as
     * HTTP 500 — confusing the client, since the fault lies in the request, not the server.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMessageNotReadable(HttpMessageNotReadableException ex,
                                                   HttpServletRequest request) {
        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("[ERROR] action=MALFORMED_REQUEST | path={} | detail={}", request.getRequestURI(), detail);
        return ErrorResponse.of("MALFORMED_REQUEST", detail, request.getRequestURI());
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────────

    /**
     * Catch-all for unexpected runtime exceptions.
     * Logs the full stack trace — only case where stack trace is written to log.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("[ERROR] action=UNEXPECTED_ERROR | path={}", request.getRequestURI(), ex);
        return ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support.",
                request.getRequestURI());
    }
}
