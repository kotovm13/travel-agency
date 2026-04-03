-- Test data with predictable values
-- All passwords: "Password1!" encoded with BCrypt
-- $2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2

-- Admin user
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('00000000-0000-0000-0000-000000000001', 'testadmin',
        '$2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2',
        'ADMIN', 'Test', 'Admin', 'testadmin@test.com', '+380501111111', 10000.00, true);

-- Manager user
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('00000000-0000-0000-0000-000000000002', 'testmanager',
        '$2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2',
        'MANAGER', 'Test', 'Manager', 'testmanager@test.com', '+380502222222', 5000.00, true);

-- Regular user with balance
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('00000000-0000-0000-0000-000000000003', 'testuser',
        '$2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2',
        'USER', 'Test', 'User', 'testuser@test.com', '+380503333333', 3000.00, true);

-- Blocked user
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('00000000-0000-0000-0000-000000000004', 'blockeduser',
        '$2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2',
        'USER', 'Blocked', 'User', 'blocked@test.com', '+380504444444', 1000.00, false);

-- User with zero balance
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('00000000-0000-0000-0000-000000000005', 'pooruser',
        '$2a$12$LJ3m4ys3uz2YHQ3MFnpXeOZCHM3sP0GXbMfPJNFKq3bRAWl0dhSe2',
        'USER', 'Poor', 'User', 'poor@test.com', '+380505555555', 0.00, true);

-- Available vouchers
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Test Hot Tour',
     'A hot tour for testing', 1000.00,
     'ADVENTURE', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-07-01', '2026-07-10', true, 10, 10),

    ('10000000-0000-0000-0000-000000000002', 'Test Regular Tour',
     'A regular tour for testing', 2000.00,
     'CULTURAL', 'TRAIN', 'THREE_STARS', 'AVAILABLE', '2026-08-01', '2026-08-07', false, 0, 5),

    ('10000000-0000-0000-0000-000000000003', 'Test Cheap Tour',
     'An affordable tour', 500.00,
     'ECO', 'BUS', 'TWO_STARS', 'AVAILABLE', '2026-09-01', '2026-09-05', false, 0, 20),

    ('10000000-0000-0000-0000-000000000004', 'Test Expensive Tour',
     'A luxury tour', 5000.00,
     'WINE', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-10-01', '2026-10-14', false, 20, 3),

    ('10000000-0000-0000-0000-000000000005', 'Test Safari Tour',
     'Safari tour for booking tests', 1500.00,
     'SAFARI', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-11-01', '2026-11-10', false, 0, 10),

    ('10000000-0000-0000-0000-000000000006', 'Test Health Tour',
     'Health tour for booking tests', 1200.00,
     'HEALTH', 'SHIP', 'THREE_STARS', 'AVAILABLE', '2026-12-01', '2026-12-08', false, 0, 5);

-- Sample bookings
INSERT INTO booking (id, user_id, voucher_id, status, booked_price, created_at)
VALUES ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000005', 'REGISTERED', 1500.00, '2026-01-15 10:00:00');

INSERT INTO booking (id, user_id, voucher_id, status, booked_price, created_at)
VALUES ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000006', 'PAID', 1200.00, '2026-01-20 14:00:00');
