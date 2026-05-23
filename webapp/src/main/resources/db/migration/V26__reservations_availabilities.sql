-- Phase 3: bridge table linking reservations to the availability rows that priced each wall-calendar
-- chunk. Reservation totals are derived from day_price * covered days in SQL, not in Java.

CREATE TABLE reservations_availabilities (
    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    availability_id INTEGER NOT NULL REFERENCES listing_availability(id),
    covered_start_date DATE NOT NULL,
    covered_end_date DATE NOT NULL,
    CONSTRAINT reservations_availabilities_covered_range_check
        CHECK (covered_end_date >= covered_start_date),
    PRIMARY KEY (reservation_id, availability_id, covered_start_date)
);

CREATE INDEX reservations_availabilities_reservation_id_idx
    ON reservations_availabilities (reservation_id);

CREATE INDEX reservations_availabilities_availability_id_idx
    ON reservations_availabilities (availability_id);

-- Backfill legacy reservations with a single row spanning the wall-calendar pickup/return days.
-- Uses the availability row that wins on the pickup day (most recent covering that day).
INSERT INTO reservations_availabilities (reservation_id, availability_id, covered_start_date, covered_end_date)
SELECT
    r.id,
    la.id,
    (r.start_date AT TIME ZONE 'America/Argentina/Buenos_Aires')::date,
    (r.end_date AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
FROM reservations r
JOIN LATERAL (
    SELECT la2.id
    FROM listing_availability la2
    WHERE la2.listing_id = r.listing_id
      AND la2.kind = 'offered'
      AND la2.start_date <= (r.end_date AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
      AND la2.end_date >= (r.start_date AT TIME ZONE 'America/Argentina/Buenos_Aires')::date
    ORDER BY la2.created_at DESC, la2.id DESC
    LIMIT 1
) la ON true;
