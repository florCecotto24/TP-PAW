ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_refund_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_refund_receipt_file_id INTEGER REFERENCES stored_files(id);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS pending_refund_email_sent BOOLEAN NOT NULL DEFAULT FALSE;
