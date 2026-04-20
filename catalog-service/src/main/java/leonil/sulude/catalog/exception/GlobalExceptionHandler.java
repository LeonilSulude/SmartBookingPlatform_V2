package leonil.sulude.catalog.exception;

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
     * Handles errors that occur while parsing the HTTP request body.
     *
     * This typically happens when Jackson cannot deserialize the incoming JSON,
     * such as malformed payloads, type mismatches, or errors thrown by custom
     * deserializers (e.g. invalid enum values).
     *
     * If the root cause is an IllegalArgumentException coming from a custom
     * deserializer, the request is treated as invalid input. Otherwise, the
     * request is considered malformed JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidJson(HttpMessageNotReadableException ex) {

        Throwable cause = ex.getMostSpecificCause();

        // If the root cause is an IllegalArgumentException from our custom deserializer
        if (cause instanceof IllegalArgumentException illegalArgument) {

            ApiError apiError = new ApiError(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    "Invalid input",
                    LocalDateTime.now(),
                    List.of(
                            new FieldErrorDetails(
                                    "category",
                                    null,
                                    illegalArgument.getMessage()
                            )
                    )
            );

            return ResponseEntity.badRequest().body(apiError);
        }

        // Fallback for malformed JSON
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Malformed request",
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles IllegalArgumentException thrown by the application during request
     * processing, usually caused by invalid input detected in services or
     * business logic.
     *
     * Returns a standardized 400 Bad Request response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid input",
                LocalDateTime.now(),
                List.of(
                        new FieldErrorDetails(
                                "category",
                                null,
                                ex.getMessage()
                        )
                )
        );

        return ResponseEntity.badRequest().body(apiError);
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
}
