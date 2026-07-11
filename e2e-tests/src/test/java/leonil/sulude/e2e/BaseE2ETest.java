package leonil.sulude.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for end-to-end tests. Assumes the full platform is already running
 * via start-platform.sh --- this module orchestrates nothing itself, it is a plain
 * external HTTP client hitting the real API Gateway, exactly as a real user would.
 *
 * No Testcontainers, no Spring context, no mocks anywhere in this module --- that is
 * the entire point of an e2e suite: every service, every database, every message
 * broker is the real thing, wired together exactly as in a real deployment.
 */
public abstract class BaseE2ETest {

    protected static final String GATEWAY_URL = "http://localhost:8080";

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = GATEWAY_URL;
    }
}