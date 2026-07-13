package leonil.sulude.auth.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperties {


    private String secret;      // Gets "security.jwt.secret" from the JWT_SECRET env var
    private long expiration;    // Gets "security.jwt.expiration" from yaml

}
