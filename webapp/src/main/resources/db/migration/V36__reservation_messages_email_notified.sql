ALTER TABLE reservation_messages
    ADD COLUMN IF NOT EXISTS email_notified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE reservation_messages SET email_notified = TRUE;

CREATE INDEX IF NOT EXISTS idx_reservation_messages_email_pending
    ON reservation_messages (email_notified, created_at);
