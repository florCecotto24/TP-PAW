-- Reservation: granular cancellation reasons (legacy 'cancelled' unchanged).
-- Listing: paused_due_to_lack_of_cbu reserved for lack of CBU enforcement (distinct from user 'paused').

ALTER TABLE reservations ALTER COLUMN status TYPE VARCHAR(40);

ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_status_check;

ALTER TABLE reservations ADD CONSTRAINT reservations_status_check
    CHECK (status IN (
        'pending',
        'accepted',
        'started',
        'cancelled',
        'cancelled_by_rider',
        'cancelled_by_owner',
        'cancelled_due_to_missing_payment_proof',
        'finished'
    ));

ALTER TABLE listings DROP CONSTRAINT IF EXISTS listings_status_check;

ALTER TABLE listings ALTER COLUMN status TYPE VARCHAR(40);

ALTER TABLE listings ADD CONSTRAINT listings_status_check
    CHECK (status IN ('active', 'paused', 'finished', 'paused_due_to_lack_of_cbu'));
