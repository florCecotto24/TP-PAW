ALTER TABLE reservations ADD COLUMN IF NOT EXISTS total_price DECIMAL(10, 2);

UPDATE reservations r
SET total_price = (
    SELECT l.day_price * CEIL(EXTRACT(EPOCH FROM (r.end_date - r.start_date)) / 86400.0)
    FROM listings l
    WHERE l.id = r.listing_id
);

ALTER TABLE reservations ALTER COLUMN total_price SET NOT NULL;
