-- Car brand/model catalog, cars extensions, user role assignment audit, saved listings, reservation review deadline

CREATE TABLE car_brands (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE car_models (
    id SERIAL PRIMARY KEY,
    brand_id INTEGER NOT NULL REFERENCES car_brands (id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    validated BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE cars ADD CONSTRAINT cars_owner_id_plate_key UNIQUE (owner_id, plate);

ALTER TABLE cars
    ADD COLUMN new_model INTEGER REFERENCES car_models (id) ON UPDATE CASCADE;

ALTER TABLE cars
    ADD COLUMN insurance_file_id INTEGER REFERENCES stored_files (id) ON DELETE SET NULL;

ALTER TABLE cars
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'active',
    ADD CONSTRAINT cars_status_check CHECK (status IN ('active', 'paused_due_to_lack_of_insurance'));

ALTER TABLE cars
    ADD CONSTRAINT cars_powertrain_check CHECK (powertrain IN ('GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG'));

ALTER TABLE user_roles
    ADD COLUMN assigned_by INTEGER REFERENCES users (id) ON DELETE SET NULL;

CREATE TABLE saved_listings (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    listing_id INTEGER NOT NULL REFERENCES listings (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, listing_id)
);

ALTER TABLE reservations
    ADD COLUMN review_deadline TIMESTAMPTZ;
