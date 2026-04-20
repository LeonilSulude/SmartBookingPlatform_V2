package leonil.sulude.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Jackson configuration.
 *
 * Defines a single ObjectMapper bean to be reused across the application
 * (RabbitMQ listeners, REST controllers, tests).
 *
 * This avoids multiple ObjectMapper instances with inconsistent behavior
 * and makes it easier to evolve JSON handling in future versions
 * (custom date formats, masking, tracing, etc.).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {

        ObjectMapper mapper = new ObjectMapper();

        // Support for Java 8+ date/time types (Instant, LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Use ISO-8601 instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}

