-- Phase 7a: decouple reservations and listing_availability from the listings table by making
-- listing_id nullable in both tables. Existing rows keep their listing_id values; new rows
-- created after 7b/7c will leave listing_id NULL and rely on car_id exclusively.

ALTER TABLE reservations         ALTER COLUMN listing_id DROP NOT NULL;
ALTER TABLE listing_availability ALTER COLUMN listing_id DROP NOT NULL;
