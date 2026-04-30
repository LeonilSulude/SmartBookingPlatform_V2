-- local cache of stable resource data consumed from Kafka events published by the Catalog Service
-- prevents synchronous calls to Catalog for data that rarely changes (name, price, duration)
CREATE TABLE IF NOT EXISTS resource_cache (
    id                  UUID           PRIMARY KEY,
    name                VARCHAR(255)   NOT NULL,
    price               NUMERIC(19, 2) NOT NULL,
    duration_in_minutes INTEGER,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    last_updated        TIMESTAMP      NOT NULL
);
