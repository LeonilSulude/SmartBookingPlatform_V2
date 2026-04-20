package leonil.sulude.booking.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import leonil.sulude.booking.model.ServiceCategory;

import java.io.IOException;

public class ServiceCategoryDeserializer extends JsonDeserializer<ServiceCategory> {
    @Override
    public ServiceCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        for (ServiceCategory category : ServiceCategory.values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid category: " + value);
    }
}
