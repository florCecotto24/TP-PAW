ALTER TABLE listings ADD COLUMN IF NOT EXISTS rating_avg NUMERIC(4, 2);

ALTER TABLE reservations ADD COLUMN IF NOT EXISTS return_reminder_email_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS return_checkout_email_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS rider_review_invite_email_sent BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'USER'
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'USER');
