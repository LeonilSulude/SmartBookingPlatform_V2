-- bookings: records of customer reservations for a catalog resource
CREATE TABLE IF NOT EXISTS bookings (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id    UUID         NOT NULL,
    customer_name  VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    start_time     TIMESTAMP    NOT NULL,
    end_time       TIMESTAMP    NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL
);
