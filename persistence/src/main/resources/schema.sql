-- REFERENCE SCHEMA: use as a reference for the current database schema.

CREATE TABLE IF NOT EXISTS users(
    id SERIAL PRIMARY KEY,
    email VARCHAR(50) UNIQUE NOT NULL,
    forename VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255),
    email_validated BOOLEAN,
    phone_number VARCHAR(20),
    birth_date DATE,
    about TEXT,
    profile_picture_id INTEGER,
    latest_locale VARCHAR(32),
    license_file_id INTEGER,
    license_validated BOOLEAN NOT NULL DEFAULT FALSE,
    identity_file_id INTEGER,
    identity_validated BOOLEAN NOT NULL DEFAULT FALSE,
    cbu VARCHAR(22),
    rating_as_rider NUMERIC(4, 2),
    rating_as_owner NUMERIC(4, 2),
    member_since DATE
);

CREATE TABLE IF NOT EXISTS cars (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    plate VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    transmission VARCHAR(50) NOT NULL,
    powertrain VARCHAR(50) NOT NULL,

    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS neighborhoods (
    id SERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE
);

INSERT INTO neighborhoods (id, name) VALUES
    (1, 'Agronomía'),
    (2, 'Almagro'),
    (3, 'Balvanera'),
    (4, 'Barracas'),
    (5, 'Belgrano'),
    (6, 'Boedo'),
    (7, 'Caballito'),
    (8, 'Chacarita'),
    (9, 'Coghlan'),
    (10, 'Colegiales'),
    (11, 'Constitución'),
    (12, 'Flores'),
    (13, 'Floresta'),
    (14, 'La Boca'),
    (15, 'La Paternal'),
    (16, 'Liniers'),
    (17, 'Mataderos'),
    (18, 'Monte Castro'),
    (19, 'Montserrat'),
    (20, 'Nueva Pompeya'),
    (21, 'Núñez'),
    (22, 'Palermo'),
    (23, 'Parque Avellaneda'),
    (24, 'Parque Chacabuco'),
    (25, 'Parque Chas'),
    (26, 'Parque Patricios'),
    (27, 'Puerto Madero'),
    (28, 'Recoleta'),
    (29, 'Retiro'),
    (30, 'Saavedra'),
    (31, 'San Cristóbal'),
    (32, 'San Nicolás'),
    (33, 'San Telmo'),
    (34, 'Vélez Sarsfield'),
    (35, 'Versalles'),
    (36, 'Villa Crespo'),
    (37, 'Villa del Parque'),
    (38, 'Villa Devoto'),
    (39, 'Villa General Mitre'),
    (40, 'Villa Lugano'),
    (41, 'Villa Luro'),
    (42, 'Villa Ortúzar'),
    (43, 'Villa Pueyrredón'),
    (44, 'Villa Real'),
    (45, 'Villa Riachuelo'),
    (46, 'Villa Santa Rita'),
    (47, 'Villa Soldati'),
    (48, 'Villa Urquiza')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('neighborhoods', 'id'),
    COALESCE((SELECT MAX(id) FROM neighborhoods), 1)
);

CREATE TABLE IF NOT EXISTS listings (
    id SERIAL PRIMARY KEY,
    title VARCHAR(105) NOT NULL,
    car_id INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('active', 'paused', 'finished', 'paused_due_to_lack_of_cbu')),
    day_price DECIMAL(10, 2) NOT NULL,
    start_point_street TEXT NOT NULL,
    start_point_number VARCHAR(10) NOT NULL DEFAULT '',
    description VARCHAR(200) NOT NULL,
    check_in_time TIME NOT NULL,
    check_out_time TIME NOT NULL,
    neighborhood_id INTEGER,
    rating_avg NUMERIC(4, 2),

    FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE,
    FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS listing_availability(
    id SERIAL PRIMARY KEY,
    listing_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date >= start_date),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS images (
    id SERIAL PRIMARY KEY,
    image_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    byte_array BYTEA NOT NULL
);

CREATE TABLE IF NOT EXISTS car_pictures (
    id SERIAL PRIMARY KEY,
    car_id INTEGER NOT NULL,
    image_id INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    UNIQUE (car_id, display_order),
    FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stored_files (
    id SERIAL PRIMARY KEY,
    uploader_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    byte_array BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    rider_id INTEGER NOT NULL,
    listing_id INTEGER NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN (
        'pending',
        'accepted',
        'started',
        'cancelled',
        'cancelled_by_rider',
        'cancelled_by_owner',
        'cancelled_due_to_missing_payment_proof',
        'finished'
    )),
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    payment_receipt_file_id INTEGER REFERENCES stored_files(id),
    payment_approved BOOLEAN NOT NULL DEFAULT FALSE,
    payment_proof_deadline_at TIMESTAMPTZ,
    car_returned BOOLEAN NOT NULL DEFAULT FALSE,
    return_reminder_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    return_checkout_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    rider_review_invite_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    pending_paymentproof_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    payment_refund_required BOOLEAN NOT NULL DEFAULT FALSE,
    payment_refund_receipt_file_id INTEGER REFERENCES stored_files(id),
    pending_refund_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    refund_proof_deadline_at TIMESTAMPTZ,
    payment_refund_approved BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (rider_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    reservation_id INTEGER NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    made_by_rider BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    rating INTEGER CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5)),
    comment TEXT,
    PRIMARY KEY (reservation_id, made_by_rider)
);

ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_profile_picture_id;
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_license_file_id;
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_identity_file_id;
ALTER TABLE users ADD CONSTRAINT fk_users_profile_picture_id FOREIGN KEY (profile_picture_id) REFERENCES images(id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_license_file_id FOREIGN KEY (license_file_id) REFERENCES stored_files(id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_identity_file_id FOREIGN KEY (identity_file_id) REFERENCES stored_files(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS email_verification_codes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verification_codes_user_id ON email_verification_codes (user_id);

CREATE TABLE IF NOT EXISTS password_reset_codes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_password_reset_codes_user_id ON password_reset_codes (user_id);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
