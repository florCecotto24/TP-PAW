-- Credentials epoch: bumped whenever the password hash changes so previously issued
-- JWTs (especially long-lived refresh tokens) stop authenticating after change/reset.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_version INTEGER NOT NULL DEFAULT 0;
