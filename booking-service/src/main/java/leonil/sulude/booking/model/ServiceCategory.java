package leonil.sulude.booking.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import leonil.sulude.booking.util.ServiceCategoryDeserializer;

@JsonDeserialize(using = ServiceCategoryDeserializer.class)
public enum ServiceCategory {
    EDUCATION,
    HEALTH,
    FITNESS,
    CONSULTING,
    TECHNOLOGY,
    BEAUTY,
    OTHER
}


