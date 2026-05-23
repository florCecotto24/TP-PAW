-- Phase 6: add model_id FK column to cars, make legacy brand/model string columns nullable,
-- and drop the obsolete new_model column (added in V20, superseded by model_id).
-- The data migration (populating model_id from the brand/model strings) is performed manually
-- on the database so that matches can be reviewed and corrected before the FK is enforced.

-- ── 1. Drop the obsolete new_model column (was never used by any entity mapping) ────────────────
ALTER TABLE cars DROP CONSTRAINT IF EXISTS cars_new_model_fkey;
ALTER TABLE cars DROP COLUMN IF EXISTS new_model;

-- ── 2. Add model_id column (nullable; will be populated manually) ─────────────────────────────
ALTER TABLE cars ADD COLUMN model_id INTEGER;

-- ── 3. Make legacy string columns nullable (code no longer writes to them) ───────────────────
ALTER TABLE cars ALTER COLUMN brand DROP NOT NULL;
ALTER TABLE cars ALTER COLUMN model DROP NOT NULL;

-- ── 4. Add FK constraint and index ───────────────────────────────────────────────────────────
ALTER TABLE cars
    ADD CONSTRAINT fk_cars_model_id FOREIGN KEY (model_id) REFERENCES car_models(id);

CREATE INDEX cars_model_id_idx ON cars (model_id);
