package leonil.sulude.log.messaging;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // Topic exchange where all application logs are published
    public static final String LOG_EXCHANGE = "app.logs.exchange";

    // Queue consumed by log-service to persist log events
    public static final String LOG_QUEUE = "app.logs.queue";

    // Routing key pattern to receive logs from all services
    public static final String LOG_ROUTING_KEY = "app.logs.#";

}
