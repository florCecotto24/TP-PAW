-- Mirrors car_models.validated for car brands. Existing seeded brands are assumed validated;
-- brands later created on the fly via the publish-car "Other" flow are inserted with FALSE.

ALTER TABLE car_brands
    ADD COLUMN validated BOOLEAN NOT NULL DEFAULT TRUE;
