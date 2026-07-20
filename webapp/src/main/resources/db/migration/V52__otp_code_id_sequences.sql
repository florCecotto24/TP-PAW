-- Align the two OTP code tables with the per-table sequence strategy every other entity uses
-- (JPA-10): their PKs move from IDENTITY to GenerationType.SEQUENCE. email_verification_codes.id
-- was SERIAL (V3), so its sequence already exists; IF NOT EXISTS covers both cases, and setval
-- realigns each sequence past any existing rows so explicit-id inserts cannot collide.
CREATE SEQUENCE IF NOT EXISTS email_verification_codes_id_seq AS BIGINT;
CREATE SEQUENCE IF NOT EXISTS password_reset_codes_id_seq AS BIGINT;
SELECT setval('email_verification_codes_id_seq',
              COALESCE((SELECT MAX(id) + 1 FROM email_verification_codes), 1), false);
SELECT setval('password_reset_codes_id_seq',
              COALESCE((SELECT MAX(id) + 1 FROM password_reset_codes), 1), false);
