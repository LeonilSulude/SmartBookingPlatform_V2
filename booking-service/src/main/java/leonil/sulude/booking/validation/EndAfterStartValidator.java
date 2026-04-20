package leonil.sulude.booking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import leonil.sulude.booking.dto.BookingRequestDTO;

public class EndAfterStartValidator implements ConstraintValidator<EndAfterStart, BookingRequestDTO> {

    @Override
    public boolean isValid(BookingRequestDTO dto, ConstraintValidatorContext context) {
        if (dto.startTime() == null || dto.endTime() == null) {
            return true;
        }

        if (!dto.endTime().isAfter(dto.startTime())) {
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate("End time must be after start time")
                    .addPropertyNode("endTime")   //  Associate the field
                    .addConstraintViolation();

            return false;
        }

        return true;
    }

}

