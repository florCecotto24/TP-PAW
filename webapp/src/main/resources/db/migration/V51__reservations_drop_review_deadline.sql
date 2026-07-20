-- V20 added reservations.review_deadline but no code ever read it: the review window is derived
-- at read time from end_date / car_returned_at plus app.reservation.review-auto-skip-days.
-- Drop the orphan column (schema-hsqldb.sql never carried it, so test schemas stay in sync).
ALTER TABLE reservations
    DROP COLUMN IF EXISTS review_deadline;
