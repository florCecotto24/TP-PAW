-- Drop legacy denormalized columns from cars: the body type lives in car_models,
-- and the brand/model names live in car_models + car_brands via car_models.brand_id.
-- Also add a nullable cars.manufacture_year for the optional manufacture year (>= 1886).
ALTER TABLE cars DROP COLUMN IF EXISTS type;
ALTER TABLE cars DROP COLUMN IF EXISTS brand;
ALTER TABLE cars DROP COLUMN IF EXISTS model;

ALTER TABLE cars ADD COLUMN manufacture_year INTEGER;

ALTER TABLE cars ADD CONSTRAINT cars_manufacture_year_check
    CHECK (manufacture_year IS NULL OR manufacture_year >= 1886);
