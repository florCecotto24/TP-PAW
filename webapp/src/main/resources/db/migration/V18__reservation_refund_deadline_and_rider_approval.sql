-- Refund proof deadline for UI/mails/reminder job; rider validation flag.
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS refund_proof_deadline_at TIMESTAMPTZ;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_refund_approved BOOLEAN NOT NULL DEFAULT FALSE;
