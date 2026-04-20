package leonil.sulude.booking.logging;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // Exchange
    public static final String LOG_EXCHANGE = "app.logs.exchange";

    // Routing key specific to booking-service
    public static final String LOG_ROUTING_KEY = "app.logs.booking";
}


