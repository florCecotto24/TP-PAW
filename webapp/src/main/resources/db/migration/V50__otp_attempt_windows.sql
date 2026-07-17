-- Persist OTP failed-attempt windows across JVM restarts / multi-node (N-04).
-- Keyed by normalized email, independent of which code table holds the active OTP.

CREATE TABLE otp_attempt_windows (
    email_key VARCHAR(255) PRIMARY KEY,
    failures INTEGER NOT NULL DEFAULT 0,
    window_start TIMESTAMPTZ NOT NULL
);
