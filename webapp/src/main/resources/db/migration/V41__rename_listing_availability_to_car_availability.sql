-- V41: Rename `listing_availability` table to `car_availability`.
--
-- Phase 7f of the car-centric refactor (continues the work started by V30__drop_listings_layer):
-- the `listings` table itself was dropped back then but the historical naming carried over to
-- this table and its associated index/sequence. With the codebase fully migrated to
-- `CarAvailability` (entity), `CarAvailabilityService`, etc., the table now follows suit so the
-- physical schema matches the conceptual model "a Car has zero or more availability periods".

ALTER TABLE IF EXISTS listing_availability RENAME TO car_availability;

ALTER INDEX IF EXISTS listing_availability_car_id_lookup RENAME TO car_availability_car_id_lookup;

ALTER SEQUENCE IF EXISTS listing_availability_id_seq RENAME TO car_availability_id_seq;
