-- User profile documents: insurance belongs to vehicles, not users.
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_insurance_file_id;
ALTER TABLE users DROP COLUMN IF EXISTS insurance_file_id;
ALTER TABLE users DROP COLUMN IF EXISTS insurance_validated;
