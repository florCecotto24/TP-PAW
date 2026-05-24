-- Phase 7e: reservations_availabilities becomes a pure N:N bridge between reservations and the
-- listing_availability rows considered when pricing them. The per-day winning row is resolved at
-- read time by date range + MAX(created_at), so the persisted covered_*_date columns are dropped
-- and the primary key collapses to (reservation_id, availability_id).

-- Collapse any pre-existing duplicate chunks for the same (reservation_id, availability_id) into
-- a single row. We do this by deleting all rows and re-inserting the distinct pairs so the new
-- primary key is satisfied.
CREATE TEMP TABLE _ra_distinct AS
    SELECT DISTINCT reservation_id, availability_id
    FROM reservations_availabilities;

DELETE FROM reservations_availabilities;

ALTER TABLE reservations_availabilities DROP CONSTRAINT reservations_availabilities_pkey;
ALTER TABLE reservations_availabilities DROP CONSTRAINT reservations_availabilities_covered_range_check;
ALTER TABLE reservations_availabilities DROP COLUMN covered_start_date;
ALTER TABLE reservations_availabilities DROP COLUMN covered_end_date;
ALTER TABLE reservations_availabilities ADD PRIMARY KEY (reservation_id, availability_id);

INSERT INTO reservations_availabilities (reservation_id, availability_id)
SELECT reservation_id, availability_id FROM _ra_distinct;

DROP TABLE _ra_distinct;
