package leonil.sulude.booking.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import leonil.sulude.booking.util.BookingStatusDeserializer;

@JsonDeserialize(using = BookingStatusDeserializer.class)
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
