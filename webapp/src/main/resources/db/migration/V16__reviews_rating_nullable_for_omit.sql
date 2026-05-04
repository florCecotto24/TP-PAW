-- Omitted reviews: persist a reviews row with rating NULL.
ALTER TABLE reviews ALTER COLUMN rating DROP NOT NULL;
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_rating_check;
ALTER TABLE reviews ADD CONSTRAINT reviews_rating_check CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5));
