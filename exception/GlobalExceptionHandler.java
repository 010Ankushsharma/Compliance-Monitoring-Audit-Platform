package com.company.compliance.exception;

import com.company.compliance.dto.common.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Global exception handler — intercepts all exceptions thrown from controllers
 * and service layer, mapping them to structured {@link ErrorResponse} JSON bodies.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Never expose stack traces in responses (production-safe)</li>
 *   <li>Never expose database column names or internal package paths</li>
 *   <li>Log at ERROR for unexpected errors, WARN for expected business errors</li>
 *   <li>Distinct HTTP status and error code for every exception type</li>
 *   <li>Field-level validation errors listed per field for easy client rendering</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/GlobalExceptionHandler.java}
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ── Domain exceptions (hierarchy) ─────────────────────────────

    /**
     * Handles all custom {@link CompliancePlatformException} subclasses.
     * Each subclass carries its own HTTP status and error code.
     */
    @ExceptionHandler(CompliancePlatformException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            CompliancePlatformException ex, HttpServletRequest request) {

        HttpStatus status = ex.getHttpStatus();

        // Log at WARN for expected business errors, ERROR for server-side failures
        if (status.is5xxServerError()) {
            log.error("Domain exception [{}] at {}: {}", ex.getErrorCode(),
                    request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Domain exception [{}] at {}: {}", ex.getErrorCode(),
                    request.getRequestURI(), ex.getMessage());
        }

        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Validation — @Valid on @RequestBody ───────────────────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fe.getField(), k -> new ArrayList<>())
                    .add(fe.getDefaultMessage());
        }

        String path = extractPath(request);
        log.warn("Validation failed at {}: {} field error(s)", path, fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("VALIDATION_ERROR")
                        .message("Request validation failed — check fieldErrors for details")
                        .path(path)
                        .timestamp(OffsetDateTime.now())
                        .requestId(extractRequestId(request))
                        .fieldErrors(fieldErrors)
                        .build());
    }

    // ── Validation — @Validated on @RequestParam / @PathVariable ──

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            String field = cv.getPropertyPath().toString();
            // Strip method name prefix (e.g. "listPolicies.page" → "page")
            if (field.contains(".")) field = field.substring(field.lastIndexOf('.') + 1);
            fieldErrors.computeIfAbsent(field, k -> new ArrayList<>())
                    .add(cv.getMessage());
        }

        log.warn("Constraint violation at {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("VALIDATION_ERROR")
                        .message("Request parameter validation failed")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .fieldErrors(fieldErrors)
                        .build());
    }

    // ── Malformed JSON body ────────────────────────────────────────

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = extractPath(request);
        log.warn("Malformed JSON at {}: {}", path, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("MALFORMED_REQUEST")
                        .message("Request body is malformed or contains invalid JSON")
                        .path(path)
                        .timestamp(OffsetDateTime.now())
                        .requestId(extractRequestId(request))
                        .build());
    }

    // ── Missing required request parameter ───────────────────────

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("MISSING_PARAMETER")
                        .message("Required parameter '" + ex.getParameterName() + "' is missing")
                        .path(extractPath(request))
                        .timestamp(OffsetDateTime.now())
                        .requestId(extractRequestId(request))
                        .build());
    }

    // ── Type mismatch (@PathVariable / @RequestParam) ─────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = "Parameter '" + ex.getName() + "' must be of type "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("TYPE_MISMATCH")
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Spring Security — 401 ─────────────────────────────────────

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Bad credentials at {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("INVALID_CREDENTIALS")
                        .message("Invalid email or password")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("ACCOUNT_LOCKED")
                        .message("Account is temporarily locked due to multiple failed login attempts")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("ACCOUNT_DISABLED")
                        .message("Account has been disabled. Contact your administrator.")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Spring Security — 403 ─────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied at {} for principal in request", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .status(403)
                        .error("FORBIDDEN")
                        .message("You do not have permission to access this resource")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── JWT exceptions ────────────────────────────────────────────

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("TOKEN_EXPIRED")
                        .message("JWT token has expired. Please refresh your token.")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {

        log.warn("JWT processing error at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(401)
                        .error("INVALID_TOKEN")
                        .message("JWT token is invalid or malformed")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Database integrity violations ─────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        // Log the real cause but expose nothing about DB structure
        log.error("Data integrity violation at {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        String message = "The request conflicts with existing data";
        if (ex.getMostSpecificCause().getMessage() != null
                && ex.getMostSpecificCause().getMessage().contains("unique")) {
            message = "A resource with this identifier already exists";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(409)
                        .error("DATA_CONFLICT")
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Immutability guard ────────────────────────────────────────

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupported(
            UnsupportedOperationException ex, HttpServletRequest request) {

        log.warn("Unsupported operation at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ErrorResponse.builder()
                        .status(405)
                        .error("OPERATION_NOT_SUPPORTED")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Catch-all — 500 Internal Server Error ─────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        // Always log unexpected errors with full stack trace for ops investigation
        log.error("Unhandled exception at {}: {}", request.getRequestURI(),
                ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(500)
                        .error("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred. "
                               + "Reference requestId for support.")
                        .path(request.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .requestId(request.getHeader("X-Request-ID"))
                        .build());
    }

    // ── Private helpers ───────────────────────────────────────────

    private String extractPath(WebRequest request) {
        String desc = request.getDescription(false); // e.g. "uri=/api/v1/policies"
        return desc.startsWith("uri=") ? desc.substring(4) : desc;
    }

    private String extractRequestId(WebRequest request) {
        return request.getHeader("X-Request-ID");
    }
}
