-- Store password-reset OTPs as SHA-256 hex digests (64 chars), not plaintext.
-- Active codes are ephemeral; drop any leftover plaintext rows before widening the column.
DELETE FROM password_reset_codes;

ALTER TABLE password_reset_codes
    ALTER COLUMN code TYPE VARCHAR(64);
