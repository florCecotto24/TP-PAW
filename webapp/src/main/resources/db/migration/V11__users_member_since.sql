ALTER TABLE users
    ADD COLUMN IF NOT EXISTS member_since DATE;

UPDATE users
SET member_since = DATE '2026-04-01'
WHERE member_since IS NULL;

ALTER TABLE users
    ALTER COLUMN member_since SET NOT NULL;


