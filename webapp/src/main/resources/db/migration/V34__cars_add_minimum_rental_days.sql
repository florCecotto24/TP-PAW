ALTER TABLE cars
    ADD COLUMN minimum_rental_days INTEGER NOT NULL DEFAULT 1;

ALTER TABLE cars
    ADD CONSTRAINT cars_minimum_rental_days_positive
    CHECK (minimum_rental_days >= 1);
