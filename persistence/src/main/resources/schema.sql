CREATE TABLE IF NOT EXISTS users(
    id SERIAL PRIMARY KEY,
    email VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS cars (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL REFERENCES users(id),
    plate VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    transmission VARCHAR(50) NOT NULL,
    powertrain VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS listings (
    id SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    car_id INTEGER NOT NULL REFERENCES cars(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('active', 'paused', 'finished')),
    day_price DECIMAL(10, 2) NOT NULL,
    start_point VARCHAR(50) NOT NULL,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    rider_id INTEGER NOT NULL REFERENCES users(id),
    listing_id INTEGER NOT NULL REFERENCES listings(id),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('accepted', 'started', 'cancelled', 'finished')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

<<<<<<< HEAD
CREATE TABLE IF NOT EXISTS images (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL
);

CREATE TABLE IF NOT EXISTS car_pictures (
    id SERIAL PRIMARY KEY,
    car_id INTEGER NOT NULL REFERENCES cars(id),
    image_id INTEGER NOT NULL REFERENCES images(id),
    display_order INTEGER NOT NULL
);

-- mock data para testear mailing
INSERT INTO users (email, name)
SELECT 'julian.owner@demo.local', 'Julian S.'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'julian.owner@demo.local');

INSERT INTO cars (owner_id, plate, brand, model, type, transmission, powertrain)
SELECT u.id, 'DEMO-001', 'Mercedes-Benz', 'E-Class 300', 'Sedan', 'Automatic', 'Gasoline'
FROM users u
WHERE u.email = 'julian.owner@demo.local'
  AND NOT EXISTS (
    SELECT 1 FROM cars c
    INNER JOIN users u2 ON c.owner_id = u2.id
    WHERE u2.email = 'julian.owner@demo.local'
  );

INSERT INTO listings (title, car_id, created_at, updated_at, status, day_price, start_point, description)
SELECT 'Mercedes-Benz E-Class 300', c.id, NOW(), NOW(), 'active', 120.00, 'Córdoba, AR', 'Listing demo — Mercedes E-Class en Córdoba.'
FROM cars c
INNER JOIN users u ON c.owner_id = u.id AND u.email = 'julian.owner@demo.local'
WHERE c.plate = 'DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM listings l WHERE l.car_id = c.id);
