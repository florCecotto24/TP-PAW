-- Metadata size for chat/list paths that must not read byte_array only to measure length.
ALTER TABLE stored_files
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

UPDATE stored_files
SET size_bytes = octet_length(byte_array)
WHERE size_bytes IS NULL;

ALTER TABLE stored_files
    ALTER COLUMN size_bytes SET NOT NULL;
