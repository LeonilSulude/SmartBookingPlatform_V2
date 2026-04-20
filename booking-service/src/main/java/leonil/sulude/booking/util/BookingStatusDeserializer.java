package leonil.sulude.booking.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import leonil.sulude.booking.model.BookingStatus;

import java.io.IOException;

public class BookingStatusDeserializer extends JsonDeserializer<BookingStatus> {
    @Override
    public BookingStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        for (BookingStatus status : BookingStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid booking status: " + value);
    }
}
