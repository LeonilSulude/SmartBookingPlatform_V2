package leonil.sulude.booking.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles deserialization errors, especially for invalid enum values.
     * Example: when a request provides an unknown category.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidFormat(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // Specific handling for enum parsing errors
        if (cause instanceof InvalidFormatException formatException) {
            String field = formatException.getPath().get(0).getFieldName();
            String rejectedValue = String.valueOf(formatException.getValue());

            // Extract expected type (usually an enum) and possible values
            String targetType = formatException.getTargetType().getSimpleName();
            String validValues = "";
            if (formatException.getTargetType().isEnum()) {
                Object[] constants = formatException.getTargetType().getEnumConstants();
                validValues = "Valid options: " + List.of(constants);
            }

            FieldErrorDetails error = new FieldErrorDetails(
                    field,
                    rejectedValue,
                    "Invalid value for type " + targetType + ". " + validValues
            );

            ApiError apiError = new ApiError(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    "Invalid input",
                    LocalDateTime.now(),
                    List.of(error)
            );

            return ResponseEntity.badRequest().body(apiError);
        }

        // Generic fallback for other unreadable payloads (e.g. malformed JSON)
        ApiError fallback = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Malformed request",
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.badRequest().body(fallback);
    }

    /**
     * Handles database integrity issues like null constraint violations or duplicate keys.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage().toLowerCase();
        String userMessage;

        if (rootMessage.contains("not-null")) {
            userMessage = "One or more required fields are missing.";
        } else if (rootMessage.contains("unique") || rootMessage.contains("duplicate")) {
            userMessage = "A record with this value already exists.";
        } else {
            userMessage = "Invalid data submitted.";
        }

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Data Integrity Violation",
                userMessage,
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles generic exceptions not caught by more specific handlers.
     * Used as a last-resort fallback.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Something went wrong: " + ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    /**
     * Handles validation errors triggered by @Valid annotations (e.g. Bean Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldErrorDetails> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetails(
                        error.getField(),
                        null,  // You can add rejected value if needed
                        error.getDefaultMessage()
                ))
                .toList();

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields are invalid.",
                LocalDateTime.now(),
                validationErrors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles business rule violations that result in a conflict.
     * <p>
     * This exception is typically thrown when an operation cannot be completed
     * due to the current state of the system, for example:
     * - Attempting to book a service that is already reserved for the given time.
     * </p>
     * Returns HTTP 409 (Conflict) to indicate that the request is valid
     * but cannot be processed because it violates a business constraint.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }


    /**
     * Handles booking conflicts where a resource is already reserved for the requested time period.
     * <p>
     * This exception is thrown when a booking attempt overlaps with an existing booking,
     * indicating a violation of the availability rule.
     * </p>
     * Returns HTTP 409 (Conflict) to inform the client that the operation cannot proceed
     * due to a scheduling conflict.
     */
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiError> handleConflict(BookingConflictException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Booking Conflict",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }


    /**
     * Handles situations where a service resource is unavailable for booking.
     * <p>
     * This exception is thrown when the resource is marked as inactive or does not exist,
     * meaning it cannot be used for the requested operation.
     * </p>
     * Returns HTTP 409 (Conflict) to signal that the action is not permitted
     * due to the resource being unavailable.
     */
    @ExceptionHandler(ResourceUnavailableException.class)
    public ResponseEntity<ApiError> handleResourceUnavailable(ResourceUnavailableException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Resource Unavailable",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(apiError);
    }



}
