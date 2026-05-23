-- Phase 1 of the listing->car split: cars absorbs lifecycle status (new value set), description,
-- creation/update timestamps and rating cache. Columns are backfilled from each car's most-recent
-- listing row so existing data is preserved. The legacy CHECK constraint from V20 is replaced.

-- 1. Migrate legacy status values from V20 to the new vocabulary so the CHECK can be re-added safely.
ALTER TABLE cars DROP CONSTRAINT IF EXISTS cars_status_check;
UPDATE cars SET status = 'lack_doc' WHERE status = 'paused_due_to_lack_of_insurance';

-- 2. Overwrite the default 'active' inherited from V20 with the most-recent listing status (if any).
--    Listing statuses map as follows:
--      active                       -> active
--      paused                       -> paused
--      paused_due_to_lack_of_cbu    -> lack_doc
--      finished                     -> deactivated (owner-driven terminal state)
UPDATE cars c SET status =
    CASE l.status
        WHEN 'active' THEN 'active'
        WHEN 'paused' THEN 'paused'
        WHEN 'paused_due_to_lack_of_cbu' THEN 'lack_doc'
        WHEN 'finished' THEN 'deactivated'
        ELSE c.status
    END
FROM (
    SELECT DISTINCT ON (car_id) car_id, status, description, created_at, updated_at, rating_avg
    FROM listings
    ORDER BY car_id, created_at DESC
) l
WHERE l.car_id = c.id;

-- 3. Add the new columns nullable so the backfill can happen before the NOT NULL flip.
ALTER TABLE cars
    ADD COLUMN description VARCHAR(200),
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN rating_avg DECIMAL(4, 2);

-- 4. Backfill the new columns from each car's most-recent listing row.
UPDATE cars c SET
    description = l.description,
    created_at  = l.created_at,
    updated_at  = l.updated_at,
    rating_avg  = l.rating_avg
FROM (
    SELECT DISTINCT ON (car_id) car_id, description, created_at, updated_at, rating_avg
    FROM listings
    ORDER BY car_id, created_at DESC
) l
WHERE l.car_id = c.id;

-- 5. Cars that never had a listing get NOW() so the NOT NULL constraint can be enforced.
UPDATE cars SET created_at = NOW() WHERE created_at IS NULL;
UPDATE cars SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE cars
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- description and rating_avg stay nullable: cars created via the legacy publish flow may not have
-- a description, and rating is naturally null until reviews are accumulated.

-- 6. Replace the V20 CHECK constraint with the new lifecycle vocabulary.
ALTER TABLE cars
    ADD CONSTRAINT cars_status_check CHECK (
        status IN ('active', 'paused', 'admin_paused', 'lack_doc', 'unavailable', 'deactivated')
    );
