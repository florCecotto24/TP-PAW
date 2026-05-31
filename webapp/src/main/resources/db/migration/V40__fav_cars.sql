-- "Favorite cars" feature: each user can mark cars they want to keep an eye on.
-- The composite PK enforces idempotent toggles (a (car, user) pair exists at most once);
-- the descending index on (user_id, favorited_at) backs the "Mis favoritos" listing
-- which is ordered most-recently-favorited first and paginated at SQL level.
CREATE TABLE IF NOT EXISTS fav_cars (
    car_id INTEGER NOT NULL REFERENCES cars(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    favorited_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (car_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_fav_cars_user_favorited_at
    ON fav_cars (user_id, favorited_at DESC);
