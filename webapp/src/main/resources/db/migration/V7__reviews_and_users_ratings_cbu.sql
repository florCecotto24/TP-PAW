-- Reviews per reservation (rider vs owner) and optional user aggregates / CBU

CREATE TABLE IF NOT EXISTS reviews (
    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    made_by_rider BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    PRIMARY KEY (reservation_id, made_by_rider)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS cbu VARCHAR(22);
ALTER TABLE users ADD COLUMN IF NOT EXISTS rating_as_rider NUMERIC(4, 2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS rating_as_owner NUMERIC(4, 2);
