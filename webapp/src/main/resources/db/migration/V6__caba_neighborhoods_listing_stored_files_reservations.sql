-- CABA neighborhoods, listings (barrio + altura), payment receipt files, reservation columns and normalization
-- of the legacy start point column.

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

ALTER TABLE listings ADD COLUMN IF NOT EXISTS neighborhood_id INTEGER;

ALTER TABLE listings DROP CONSTRAINT IF EXISTS listings_neighborhood_id_fkey;

ALTER TABLE listings
    ADD CONSTRAINT listings_neighborhood_id_fkey
    FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id) ON DELETE SET NULL;

ALTER TABLE listings ADD COLUMN IF NOT EXISTS start_point_number VARCHAR(10);

UPDATE listings SET start_point_number = '' WHERE start_point_number IS NULL;

ALTER TABLE listings ALTER COLUMN start_point_number SET DEFAULT '';

ALTER TABLE listings ALTER COLUMN start_point_number SET NOT NULL;

CREATE TABLE IF NOT EXISTS stored_files (
    id SERIAL PRIMARY KEY,
    uploader_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    byte_array BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_receipt_file_id INTEGER REFERENCES stored_files(id);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_approved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_proof_deadline_at TIMESTAMPTZ;

ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_status_check;

ALTER TABLE reservations ADD CONSTRAINT reservations_status_check
    CHECK (status IN ('pending', 'accepted', 'started', 'cancelled', 'finished'));

-- Final state of pickup columns in listings: start_point_street.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point_street'
    ) THEN
        ALTER TABLE listings RENAME COLUMN start_point TO start_point_street;
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point_address'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point_street'
    ) THEN
        ALTER TABLE listings RENAME COLUMN start_point_address TO start_point_street;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'listings' AND column_name = 'start_point_street'
    ) THEN
        UPDATE listings SET start_point_street = COALESCE(start_point_street, start_point::text);
        ALTER TABLE listings DROP COLUMN start_point;
    END IF;
END $$;
