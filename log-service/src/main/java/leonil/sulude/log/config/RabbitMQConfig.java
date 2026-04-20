package leonil.sulude.log.config;

import leonil.sulude.log.messaging.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for the log-service.
 *
 * WHY THIS CLASS EXISTS:
 * ----------------------
 * Even though queues, exchanges and bindings can be created manually
 * via the RabbitMQ Management UI, relying on manual setup is fragile
 * and not reproducible.
 *
 * This configuration makes the log-service SELF-SUFFICIENT:
 * - When the service starts, it ensures that the required RabbitMQ
 *   infrastructure exists.
 * - If the queue/exchange already exists, RabbitMQ simply reuses them.
 * - If RabbitMQ is restarted, everything is recreated automatically.
 *
 * This follows the "infrastructure as code" principle.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Topic exchange used for application-wide log events.
     *
     * WHY TopicExchange:
     * ------------------
     * Topic exchanges allow routing keys with wildcards (e.g. app.logs.#),
     * which makes it easy to route logs from multiple services
     * (auth-service, catalog-service, booking-service, etc.)
     * to the same log queue.
     */
    @Bean
    public TopicExchange logExchange() {
        return new TopicExchange(
                RabbitMQConstants.LOG_EXCHANGE,
                true,   // durable: survives broker restarts
                false   // autoDelete: exchange is not deleted automatically
        );
    }

    /**
     * Queue that stores log events.
     *
     * WHY durable:
     * ------------
     * Logs are valuable for debugging and auditing.
     * A durable queue ensures messages are not lost
     * if RabbitMQ restarts.
     */
    @Bean
    public Queue logQueue() {
        return QueueBuilder
                .durable(RabbitMQConstants.LOG_QUEUE)
                .build();
    }

    /**
     * Binding between the log exchange and the log queue.
     *
     * The routing key uses a wildcard pattern (e.g. app.logs.#),
     * allowing multiple log types and services to be routed
     * into the same queue.
     *
     * Example routing keys that will match:
     * - app.logs.auth
     * - app.logs.auth.startup
     * - app.logs.catalog.error
     */
    @Bean
    public Binding logBinding() {
        return BindingBuilder
                .bind(logQueue())
                .to(logExchange())
                .with(RabbitMQConstants.LOG_ROUTING_KEY);
    }
}

