package leonil.sulude.catalog.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldErrorDetails {
    private String field;
    private String rejectedValue;
    private String message;
}
