-- add version column for optimistic locking — prevents concurrent modification of the same booking
ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
