-- service_offers: marketplace listings created by providers
CREATE TABLE IF NOT EXISTS service_offers (
                                              id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    category      VARCHAR(100) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    location      VARCHAR(255),
    created_at    TIMESTAMP   NOT NULL
    );

-- service_resources: bookable units belonging to an offer
CREATE TABLE IF NOT EXISTS service_resources (
                                                 id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id            UUID           NOT NULL REFERENCES service_offers(id),
    name                VARCHAR(255)   NOT NULL,
    price               NUMERIC(19, 2) NOT NULL,
    duration_in_minutes INTEGER,
    active              BOOLEAN        NOT NULL DEFAULT TRUE
    );

-- unavailable_periods: time blocks when a resource cannot be booked
CREATE TABLE IF NOT EXISTS unavailable_periods (
                                                   resource_id UUID      NOT NULL REFERENCES service_resources(id),
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL
    );