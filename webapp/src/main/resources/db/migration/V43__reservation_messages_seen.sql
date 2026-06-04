ALTER TABLE reservation_messages
    ADD COLUMN IF NOT EXISTS seen BOOLEAN NOT NULL DEFAULT FALSE;

DROP INDEX IF EXISTS idx_reservation_messages_email_pending;

CREATE INDEX IF NOT EXISTS idx_reservation_messages_email_pending
    ON reservation_messages (email_notified, seen, created_at);
