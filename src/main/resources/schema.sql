-- Travel Agency Database Schema

CREATE TABLE IF NOT EXISTS "user" (
    id              UUID            PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL CHECK (role IN ('USER', 'MANAGER', 'ADMIN')),
    first_name VARCHAR(50),
    last_name  VARCHAR(50),
    email      VARCHAR(100) UNIQUE,
    phone_number    VARCHAR(20),
    balance         DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    account_status  BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS voucher (
    id              UUID            PRIMARY KEY,
    title           VARCHAR(255)    NOT NULL,
    description     VARCHAR(1000),
    price           DOUBLE PRECISION NOT NULL CHECK (price > 0),
    tour_type       VARCHAR(30)     NOT NULL CHECK (tour_type IN ('HEALTH', 'SPORTS', 'LEISURE', 'SAFARI', 'WINE', 'ECO', 'ADVENTURE', 'CULTURAL')),
    transfer_type   VARCHAR(30)     NOT NULL CHECK (transfer_type IN ('BUS', 'TRAIN', 'PLANE', 'SHIP', 'PRIVATE_CAR', 'JEEPS', 'MINIBUS', 'ELECTRICAL_CARS')),
    hotel_type      VARCHAR(20)     NOT NULL CHECK (hotel_type IN ('ONE_STAR', 'TWO_STARS', 'THREE_STARS', 'FOUR_STARS', 'FIVE_STARS')),
    status   VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'DISABLED')),
    arrival_date    DATE,
    eviction_date   DATE,
    is_hot          BOOLEAN         NOT NULL DEFAULT FALSE,
    discount        INTEGER         NOT NULL DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
    quantity INTEGER     NOT NULL DEFAULT 1 CHECK (quantity >= 1),

    CONSTRAINT chk_dates CHECK (eviction_date IS NULL OR arrival_date IS NULL OR eviction_date > arrival_date)
);

CREATE TABLE IF NOT EXISTS booking
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES "user"(id),
    voucher_id UUID NOT NULL REFERENCES voucher(id),
    status       VARCHAR(20)    NOT NULL DEFAULT 'REGISTERED' CHECK (status IN ('REGISTERED', 'PAID', 'CANCELED')),
    booked_price DECIMAL(10, 2) NOT NULL CHECK (booked_price >= 0),
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_voucher_tour_type ON voucher(tour_type);
CREATE INDEX IF NOT EXISTS idx_voucher_status ON voucher(status);
CREATE INDEX IF NOT EXISTS idx_voucher_is_hot ON voucher(is_hot);
CREATE INDEX IF NOT EXISTS idx_booking_user_id ON booking(user_id);
CREATE INDEX IF NOT EXISTS idx_booking_voucher_id ON booking(voucher_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON booking(status);
