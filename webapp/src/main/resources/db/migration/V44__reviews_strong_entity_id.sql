-- Review becomes a strong entity: give it its own surrogate primary key instead of the
-- composite (reservation_id, made_by_rider). This lets every review resource served from
-- different contexts (/cars/{id}/reviews, /users/{id}/reviews, /reservations/{id}/reviews)
-- carry a single, unique URN (/reservations/{reservationId}/reviews/{reviewId}) instead of
-- aliasing the parent collection URI for every row. The one-review-per-side invariant that the
-- composite PK used to enforce is preserved as a UNIQUE constraint.
ALTER TABLE reviews ADD COLUMN id SERIAL;

ALTER TABLE reviews DROP CONSTRAINT reviews_pkey;

ALTER TABLE reviews ADD PRIMARY KEY (id);

ALTER TABLE reviews
    ADD CONSTRAINT reviews_reservation_side_unique UNIQUE (reservation_id, made_by_rider);
