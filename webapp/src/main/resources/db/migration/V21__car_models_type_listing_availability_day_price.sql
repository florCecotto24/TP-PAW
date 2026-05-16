-- Catalog model body type (same allowed values as cars.type / Car.Type) and optional price per availability segment.

ALTER TABLE car_models
    ADD COLUMN type VARCHAR(50);

UPDATE car_models SET type = 'SEDAN' WHERE type IS NULL;

ALTER TABLE car_models
    ALTER COLUMN type SET NOT NULL;

ALTER TABLE car_models
    ADD CONSTRAINT car_models_type_check CHECK (
        type IN ('SEDAN', 'HATCHBACK', 'SUV', 'COUPE', 'CONVERTIBLE', 'WAGON', 'VAN', 'PICKUP')
    );

ALTER TABLE listing_availability
    ADD COLUMN day_price DECIMAL(10, 2);
