CREATE TABLE IF NOT EXISTS users(
    id SERIAL PRIMARY KEY,
    email VARCHAR(50) UNIQUE NOT NULL,
    forename VARCHAR(50) NOT NULL,
    surname VARCHAR(50) NOT NULL
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

CREATE TABLE IF NOT EXISTS listings (
    id SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    car_id INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('active', 'paused', 'finished')),
    day_price DECIMAL(10, 2) NOT NULL,
    start_point VARCHAR(50) NOT NULL,
    description VARCHAR(200) NOT NULL,
    check_in_time TIME NOT NULL,
    check_out_time TIME NOT NULL,

    FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS listing_availability(
    id SERIAL PRIMARY KEY,
    listing_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date >= start_date),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    rider_id INTEGER NOT NULL,
    listing_id INTEGER NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('accepted', 'started', 'cancelled', 'finished')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (rider_id) REFERENCES users(id) ON DELETE CASCADE,
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
