ALTER TABLE reservations ADD COLUMN IF NOT EXISTS pending_paymentproof_email_sent BOOLEAN NOT NULL DEFAULT FALSE;

