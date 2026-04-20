package leonil.sulude.booking.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.model.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the EndAfterStart validation annotation. * * This validator ensures that the booking endTime occurs * after the startTime.
 */
class EndAfterStartValidatorTest {
    private Validator validator;

    /**
     * Initializes the Jakarta Bean Validator used for DTO validation.
     */
    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Tests that validation passes when endTime is after startTime.
     */
    @Test
    void shouldPassWhenEndAfterStart() {
        BookingRequestDTO request = new BookingRequestDTO(UUID.randomUUID(),
                "John Doe",
                "john@test.com",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                BookingStatus.PENDING);

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    /**
     * Tests that validation fails when endTime is before startTime.
     */
    @Test
    void shouldFailWhenEndBeforeStart() {
        BookingRequestDTO request = new BookingRequestDTO(UUID.randomUUID(),
                "John Doe",
                "john@test.com",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(1),
                BookingStatus.PENDING);

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    /**
     * Tests that validation fails when startTime and endTime are equal.
     */
    @Test
    void shouldFailWhenStartEqualsEnd() {
        LocalDateTime time = LocalDateTime.now().plusHours(1);
        BookingRequestDTO request = new BookingRequestDTO(UUID.randomUUID(),
                "John Doe",
                "john@test.com",
                time,
                time,
                BookingStatus.PENDING);

        Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}