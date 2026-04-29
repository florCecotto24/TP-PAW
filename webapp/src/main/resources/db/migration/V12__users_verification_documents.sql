ALTER TABLE users
    ADD COLUMN IF NOT EXISTS license_file_id INTEGER,
    ADD COLUMN IF NOT EXISTS insurance_file_id INTEGER,
    ADD COLUMN IF NOT EXISTS identity_file_id INTEGER,
    ADD COLUMN IF NOT EXISTS license_validated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS insurance_validated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS identity_validated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_license_file_id,
    DROP CONSTRAINT IF EXISTS fk_users_insurance_file_id,
    DROP CONSTRAINT IF EXISTS fk_users_identity_file_id;

ALTER TABLE users
    ADD CONSTRAINT fk_users_license_file_id FOREIGN KEY (license_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_insurance_file_id FOREIGN KEY (insurance_file_id) REFERENCES stored_files(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_identity_file_id FOREIGN KEY (identity_file_id) REFERENCES stored_files(id) ON DELETE SET NULL;
