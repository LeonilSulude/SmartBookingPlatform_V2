package leonil.sulude.catalog.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import leonil.sulude.catalog.model.ServiceCategory;

import java.io.IOException;
import java.util.Arrays;

/**
 * Custom Jackson deserializer for ServiceCategory.
 *
 * Allows case-insensitive parsing of category values and provides
 * a clearer error message when an invalid category is received.
 *
 * If the provided value does not match any known category,
 * an IllegalArgumentException is thrown and handled by the
 * GlobalExceptionHandler.
 */
public class ServiceCategoryDeserializer extends JsonDeserializer<ServiceCategory> {

    @Override
    public ServiceCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String value = p.getText();

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category cannot be null or empty.");
        }

        for (ServiceCategory category : ServiceCategory.values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }

        throw new IllegalArgumentException(
                "Invalid category: '" + value + "'. Valid options: " +
                        Arrays.toString(ServiceCategory.values())
        );
    }
}
