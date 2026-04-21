-- idempotency key — prevents duplicate bookings from client retries
ALTER TABLE bookings ADD COLUMN idempotency_key VARCHAR(255) UNIQUE;
