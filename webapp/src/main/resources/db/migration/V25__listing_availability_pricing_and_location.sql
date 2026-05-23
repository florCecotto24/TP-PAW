-- Phase 2 of the listing->car split: listing_availability absorbs pricing (day_price), pickup location
-- (street/number/neighborhood), wall check-in/out times, and a kind discriminator for "offered" vs
-- "withdrawn" rows. The "most recent created_at wins" rule lets owners edit their availability by
-- inserting new rows instead of mutating existing ones.

-- 1. Add the new columns nullable so the backfill can run before the NOT NULL flip. day_price is
--    already nullable from V21; we backfill nulls from the listing-level fallback price below.
ALTER TABLE listing_availability
    ADD COLUMN start_point_street VARCHAR(50),
    ADD COLUMN start_point_number VARCHAR(10),
    ADD COLUMN neighborhood_id INTEGER REFERENCES neighborhoods(id) ON DELETE SET NULL,
    ADD COLUMN check_in_time TIME,
    ADD COLUMN check_out_time TIME,
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'offered';

-- 2. Backfill from the owning listing (deterministic 1:1 join, not "most recent"). For day_price:
--    NULL means "fall back to listing.day_price" under the legacy semantics; we make that explicit.
UPDATE listing_availability la SET
    day_price          = COALESCE(la.day_price, l.day_price),
    start_point_street = l.start_point_street,
    start_point_number = l.start_point_number,
    neighborhood_id    = l.neighborhood_id,
    check_in_time      = l.check_in_time,
    check_out_time     = l.check_out_time
FROM listings l
WHERE l.id = la.listing_id;

-- 3. Lock down the columns that must always carry a value. neighborhood_id and start_point_number
--    stay nullable to mirror the listings schema.
ALTER TABLE listing_availability
    ALTER COLUMN day_price          SET NOT NULL,
    ALTER COLUMN start_point_street SET NOT NULL,
    ALTER COLUMN check_in_time      SET NOT NULL,
    ALTER COLUMN check_out_time     SET NOT NULL;

-- 4. Domain constraint on the kind discriminator.
ALTER TABLE listing_availability
    ADD CONSTRAINT listing_availability_kind_check CHECK (kind IN ('offered', 'withdrawn'));

-- 5. Lookup index for "the most recent availability winning for a given day" queries.
CREATE INDEX IF NOT EXISTS listing_availability_lookup
    ON listing_availability (listing_id, start_date, end_date, created_at DESC);
