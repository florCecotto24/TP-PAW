-- Phase 4 of the listing->car split: reservations, saved_listings, reviews and listing_availability
-- gain a direct car_id FK while listing_id remains for dual-read. Existing rows are backfilled from
-- listings.car_id (reviews via reservations -> listings).

-- reservations
ALTER TABLE reservations ADD COLUMN car_id INTEGER;

UPDATE reservations r
SET car_id = l.car_id
FROM listings l
WHERE l.id = r.listing_id;

ALTER TABLE reservations ALTER COLUMN car_id SET NOT NULL;

ALTER TABLE reservations
    ADD CONSTRAINT fk_reservations_car_id FOREIGN KEY (car_id) REFERENCES cars(id);

CREATE INDEX reservations_car_id_idx ON reservations (car_id);

-- saved_listings
ALTER TABLE saved_listings ADD COLUMN car_id INTEGER;

UPDATE saved_listings sl
SET car_id = l.car_id
FROM listings l
WHERE l.id = sl.listing_id;

ALTER TABLE saved_listings ALTER COLUMN car_id SET NOT NULL;

ALTER TABLE saved_listings
    ADD CONSTRAINT fk_saved_listings_car_id FOREIGN KEY (car_id) REFERENCES cars(id);

CREATE INDEX saved_listings_car_id_idx ON saved_listings (car_id);

-- reviews
ALTER TABLE reviews ADD COLUMN car_id INTEGER;

UPDATE reviews rev
SET car_id = l.car_id
FROM reservations r
JOIN listings l ON l.id = r.listing_id
WHERE r.id = rev.reservation_id;

ALTER TABLE reviews ALTER COLUMN car_id SET NOT NULL;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_car_id FOREIGN KEY (car_id) REFERENCES cars(id);

CREATE INDEX reviews_car_id_idx ON reviews (car_id);

-- listing_availability
ALTER TABLE listing_availability ADD COLUMN car_id INTEGER;

UPDATE listing_availability la
SET car_id = l.car_id
FROM listings l
WHERE l.id = la.listing_id;

ALTER TABLE listing_availability ALTER COLUMN car_id SET NOT NULL;

ALTER TABLE listing_availability
    ADD CONSTRAINT fk_listing_availability_car_id FOREIGN KEY (car_id) REFERENCES cars(id);

CREATE INDEX listing_availability_car_id_lookup
    ON listing_availability (car_id, start_date, end_date, created_at DESC);
