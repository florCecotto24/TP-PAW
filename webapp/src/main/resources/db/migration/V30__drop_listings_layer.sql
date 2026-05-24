-- V30: Eliminate the listings layer entirely.
--
-- Phase 7e of the car-centric refactor: every consumer (services, controllers, JSPs, mailers,
-- schedulers, JPA entities) now reads/writes through cars + listing_availability + reservations.car_id.
-- This migration drops the now-unused listing_id columns, the saved_listings table, and the listings table.

-- Drop FK / index references that target the listings table before dropping it.
ALTER TABLE IF EXISTS reservations DROP CONSTRAINT IF EXISTS reservations_listing_id_fkey;
ALTER TABLE IF EXISTS listing_availability DROP CONSTRAINT IF EXISTS listing_availability_listing_id_fkey;

DROP INDEX IF EXISTS listing_availability_lookup;

ALTER TABLE IF EXISTS listing_availability DROP COLUMN IF EXISTS listing_id;
ALTER TABLE IF EXISTS reservations DROP COLUMN IF EXISTS listing_id;

DROP TABLE IF EXISTS saved_listings;
DROP TABLE IF EXISTS listings;
