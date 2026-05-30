ALTER TABLE car_pictures ALTER COLUMN image_id DROP NOT NULL;

ALTER TABLE car_pictures
    ADD COLUMN IF NOT EXISTS stored_file_id INTEGER NULL REFERENCES stored_files(id) ON DELETE CASCADE;

ALTER TABLE car_pictures
    ADD CONSTRAINT car_pictures_image_or_video_chk
    CHECK (image_id IS NOT NULL OR stored_file_id IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_car_pictures_stored_file_id ON car_pictures (stored_file_id);
