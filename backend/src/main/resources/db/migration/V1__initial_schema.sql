-- V1__initial_schema.sql
-- ParkSmart Initial Database Schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================
-- USERS
-- ==============================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    password      VARCHAR(255)  NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users(email);

-- ==============================
-- VEHICLES
-- ==============================
CREATE TABLE vehicles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    license_plate  VARCHAR(20)   NOT NULL UNIQUE,
    vehicle_type   VARCHAR(30)   NOT NULL,
    brand          VARCHAR(50),
    model          VARCHAR(50),
    color          VARCHAR(20),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vehicle_type CHECK (
        vehicle_type IN ('MOTORCYCLE','CAR','SUV','TRUCK','ELECTRIC_CAR','BUS')
    )
);

CREATE INDEX idx_vehicles_user ON vehicles(user_id);
CREATE INDEX idx_vehicles_plate ON vehicles(license_plate);

-- ==============================
-- PARKING LOTS
-- ==============================
CREATE TABLE parking_lots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100)  NOT NULL,
    address       VARCHAR(255)  NOT NULL,
    city          VARCHAR(100),
    zip_code      VARCHAR(20),
    total_floors  INT           NOT NULL DEFAULT 1,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================
-- PARKING FLOORS
-- ==============================
CREATE TABLE parking_floors (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id        UUID          NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    floor_number  INT           NOT NULL,
    floor_name    VARCHAR(50),
    CONSTRAINT uq_floor_number_lot UNIQUE(lot_id, floor_number)
);

CREATE INDEX idx_floors_lot ON parking_floors(lot_id);

-- ==============================
-- PARKING SLOTS
-- ==============================
CREATE TABLE parking_slots (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id              UUID          NOT NULL REFERENCES parking_floors(id) ON DELETE CASCADE,
    slot_number           VARCHAR(10)   NOT NULL,
    slot_type             VARCHAR(20)   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    has_ev_charger        BOOLEAN       NOT NULL DEFAULT FALSE,
    is_handicap_accessible BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slot_type CHECK (
        slot_type IN ('SMALL','MEDIUM','LARGE','EV','OVERSIZED')
    ),
    CONSTRAINT chk_slot_status CHECK (
        status IN ('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE')
    )
);

CREATE INDEX idx_slots_floor_status   ON parking_slots(floor_id, status);
CREATE INDEX idx_slots_type_status    ON parking_slots(slot_type, status);

-- ==============================
-- BOOKINGS
-- ==============================
CREATE TABLE bookings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users(id),
    vehicle_id       UUID          NOT NULL REFERENCES vehicles(id),
    slot_id          UUID          NOT NULL REFERENCES parking_slots(id),
    entry_time       TIMESTAMP     NOT NULL,
    exit_time        TIMESTAMP,
    duration_minutes BIGINT,
    status           VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_status CHECK (
        status IN ('ACTIVE','COMPLETED','CANCELLED','PENDING_PAYMENT')
    )
);

CREATE INDEX idx_bookings_user       ON bookings(user_id);
CREATE INDEX idx_bookings_slot       ON bookings(slot_id);
CREATE INDEX idx_bookings_status     ON bookings(status);
CREATE INDEX idx_bookings_entry_time ON bookings(entry_time);

-- ==============================
-- BILLS
-- ==============================
CREATE TABLE bills (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id       UUID          NOT NULL UNIQUE REFERENCES bookings(id),
    base_amount      NUMERIC(10,2) NOT NULL,
    tax_amount       NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    discount_amount  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    total_amount     NUMERIC(10,2) NOT NULL,
    pricing_strategy VARCHAR(50),
    surge_multiplier NUMERIC(4,2)  NOT NULL DEFAULT 1.00,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================
-- PAYMENTS
-- ==============================
CREATE TABLE payments (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id               UUID          NOT NULL UNIQUE REFERENCES bills(id),
    amount                NUMERIC(10,2) NOT NULL,
    currency              VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    stripe_session_id     VARCHAR(200)  UNIQUE,
    stripe_payment_intent VARCHAR(200),
    payment_link          VARCHAR(500),
    paid_at               TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING','COMPLETED','FAILED','REFUNDED')
    )
);

CREATE INDEX idx_payments_stripe_session ON payments(stripe_session_id);
CREATE INDEX idx_payments_status         ON payments(status);

-- ==============================
-- SEED: Default admin user (password: Admin@123)
-- ==============================
INSERT INTO users (name, email, password, role) VALUES
    ('Admin',
     'admin@parksmart.com',
     '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'ADMIN');

-- SEED: Sample parking lot
INSERT INTO parking_lots (name, address, city, zip_code, total_floors)
VALUES ('ParkSmart Downtown', '123 Main Street', 'San Francisco', '94102', 3);
