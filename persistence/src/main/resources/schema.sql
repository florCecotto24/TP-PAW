CREATE TABLE IF NOT EXISTS cars (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    plate VARCHAR(50) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    transmission VARCHAR(50) NOT NULL,
    powertrain VARCHAR(50) NOT NULL
);