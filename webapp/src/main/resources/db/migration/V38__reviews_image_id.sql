-- Allow attaching an optional image to a review. The image is exclusive to the review
-- (composition within the Review aggregate, DDD); enforce the 1-1 invariant with UNIQUE
-- and let Hibernate's orphanRemoval clean up the image when the review is deleted via JPA.
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS image_id INTEGER NULL REFERENCES images(id) ON DELETE SET NULL;

ALTER TABLE reviews
    ADD CONSTRAINT reviews_image_id_unique UNIQUE (image_id);
