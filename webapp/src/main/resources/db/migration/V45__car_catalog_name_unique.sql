-- Uniqueness on car_brands / car_models matches the catalog contract the API now enforces:
-- POST /brands and POST /brands/{id}/models must not silently reuse an existing name
-- (409 Conflict instead of find-or-create). Names are compared case-insensitively, so the
-- accepted row shape is one brand per LOWER(name) and one model per (brand_id, LOWER(name)).
-- Shipping that invariant as a Flyway migration keeps every database — local, Pampero, and
-- any other deploy — aligned with the Java createUnvalidated path; app-only checks would
-- leave older instances able to accumulate duplicates under concurrent "Other" publishes.

-- Re-point models from duplicate brands onto the lowest-id survivor, then drop dupes.
UPDATE car_models AS m
SET brand_id = keep.id
FROM car_brands AS keep
JOIN car_brands AS dup ON LOWER(keep.name) = LOWER(dup.name) AND keep.id < dup.id
WHERE m.brand_id = dup.id;

DELETE FROM car_brands AS dup
USING car_brands AS keep
WHERE LOWER(keep.name) = LOWER(dup.name) AND keep.id < dup.id;

-- Within a brand, keep the lowest-id model per case-insensitive name.
DELETE FROM car_models AS dup
USING car_models AS keep
WHERE dup.brand_id = keep.brand_id
  AND LOWER(dup.name) = LOWER(keep.name)
  AND keep.id < dup.id;

CREATE UNIQUE INDEX IF NOT EXISTS car_brands_name_lower_uidx ON car_brands (LOWER(name));
CREATE UNIQUE INDEX IF NOT EXISTS car_models_brand_name_lower_uidx ON car_models (brand_id, LOWER(name));
