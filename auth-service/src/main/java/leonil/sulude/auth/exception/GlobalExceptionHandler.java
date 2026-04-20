package leonil.sulude.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for centralized error responses.
 * Captures and formats validation errors, illegal arguments,
 * and any unexpected exceptions for consistent API feedback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors triggered by @Valid annotations.
     * Returns HTTP 400 with detailed field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldErrorDetails> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetails(
                        error.getField(),
                        null,
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields are invalid.",
                LocalDateTime.now(),
                errors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles business logic violations like:
     * - Email already in use
     * - Invalid credentials
     * Returns HTTP 400 with the error message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Catches all other unhandled exceptions.
     * Useful for debugging and fallback safety.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
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
     * Handles cases where a user cannot be found during authentication.
     *
     * This exception is typically thrown when a login attempt is made
     * with an email that does not exist in the system.
     *
     * Although this is an authentication failure, returning HTTP 401
     * is preferred over 404 to avoid leaking information about
     * which users exist in the system.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UsernameNotFoundException ex) {

        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    /**
     * Handles the case where a user attempts to register with an email
     * that is already associated with an existing account.
     *
     * Returns a 409 CONFLICT status with a detailed error response.
     *
     * @param ex the exception thrown when email is already in use
     * @return a structured API error response with conflict status
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailConflict(EmailAlreadyExistsException ex) {
        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Email Conflict",
                ex.getMessage(),
                LocalDateTime.now(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles authentication failures caused by invalid credentials.
     *
     * This exception is thrown when a login attempt is made
     * with incorrect email/password combination.
     *
     * Returns HTTP 401 Unauthorized to indicate
     * that authentication has failed.
     *
     * IMPORTANT:
     * The message should remain generic to avoid
     * leaking information about which field was incorrect.
     *
     * @param ex the exception thrown for invalid login attempts
     * @return structured API error response with 401 status
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {

        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now(),
                Collections.emptyList()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }
}

