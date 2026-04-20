-- log_event: centralised log entries consumed from RabbitMQ across all services
CREATE TABLE IF NOT EXISTS log_event (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id VARCHAR(255)  NOT NULL,
    service_name   VARCHAR(255),
    event_type     VARCHAR(255),
    level          VARCHAR(50),
    source         VARCHAR(255),
    message        VARCHAR(2000),
    created_at     TIMESTAMP
);

-- index for filtering logs by request flow across services
CREATE INDEX IF NOT EXISTS idx_log_correlation_id ON log_event(correlation_id);
