-- Browse/filter by rider; align status length with entity; claim flag for pickup reminders.
CREATE INDEX IF NOT EXISTS reservations_rider_id_idx ON reservations (rider_id);

ALTER TABLE reservations ALTER COLUMN status TYPE VARCHAR(60);

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS pickup_reminder_email_sent BOOLEAN NOT NULL DEFAULT FALSE;
