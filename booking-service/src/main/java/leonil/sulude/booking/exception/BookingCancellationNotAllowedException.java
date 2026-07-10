package leonil.sulude.booking.exception;

/**
 * Thrown when a booking cancellation is attempted too close to the scheduled start time.
 * Mapped to HTTP 400 — a business rule violation, distinct from HTTP 409 which is
 * reserved exclusively for optimistic locking concurrency conflicts.
 */
public class BookingCancellationNotAllowedException extends RuntimeException {
    public BookingCancellationNotAllowedException(String message) {
        super(message);
    }
}