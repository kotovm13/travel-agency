-- Test profile data for Travel Agency (H2 compatible)
-- Passwords: Password1! -> $2a$10$l3bGS/nZgJlx59YdYQczWemRux7b.cKcCyWnFBNdpR54IB7Cg9NZG

-- Admin user (password: Password1!)
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('f3e02ce0-365d-4c03-90a1-98f00cf6d3d1', 'admin',
        '$2a$10$l3bGS/nZgJlx59YdYQczWemRux7b.cKcCyWnFBNdpR54IB7Cg9NZG',
        'ADMIN', 'Admin', 'User', 'admin@travelagency.com', '+380501234567', 10000.00, true);

-- Manager user (password: Password1!)
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'manager',
        '$2a$10$l3bGS/nZgJlx59YdYQczWemRux7b.cKcCyWnFBNdpR54IB7Cg9NZG',
        'MANAGER', 'Manager', 'User', 'manager@travelagency.com', '+380509876543', 5000.00, true);

-- Regular user (password: Password1!)
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'user',
        '$2a$10$l3bGS/nZgJlx59YdYQczWemRux7b.cKcCyWnFBNdpR54IB7Cg9NZG',
        'USER', 'John', 'Doe', 'user@travelagency.com', '+380507654321', 5000.00, true);

-- ===================== TEST VOUCHERS =====================

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000001', '[TEST] Egyptian Pyramid Adventure',
        'Explore the ancient pyramids of Giza and cruise the Nile', 1500.00, 'ADVENTURE', 'PLANE', 'FOUR_STARS',
        'AVAILABLE', '2026-06-01', '2026-06-14', true, 10, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000002', '[TEST] Amazon Jungle Expedition',
        'Deep into the Amazon rainforest with experienced guides', 2800.00, 'ADVENTURE', 'PLANE', 'TWO_STARS',
        'AVAILABLE', '2026-07-01', '2026-07-12', false, 0, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000003', '[TEST] Patagonia Trekking',
        'Hike through the stunning landscapes of Patagonia', 3200.00, 'ADVENTURE', 'PLANE', 'THREE_STARS', 'AVAILABLE',
        '2026-08-10', '2026-08-22', false, 5, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000004', '[TEST] Iceland Ring Road',
        'Drive around Iceland seeing geysers waterfalls and glaciers', 2500.00, 'ADVENTURE', 'PRIVATE_CAR',
        'THREE_STARS', 'AVAILABLE', '2026-06-15', '2026-06-25', true, 15, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000005', '[TEST] Nepal Himalaya Base Camp',
        'Trek to Everest Base Camp through Sherpa villages', 1800.00, 'ADVENTURE', 'PLANE', 'ONE_STAR', 'AVAILABLE',
        '2026-09-01', '2026-09-18', false, 0, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000002-0000-0000-0000-000000000001', '[TEST] Paris Art and History',
        'Visit the Louvre Eiffel Tower and Versailles', 2200.00, 'CULTURAL', 'PLANE', 'FIVE_STARS', 'AVAILABLE',
        '2026-07-10', '2026-07-17', false, 0, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000002-0000-0000-0000-000000000002', '[TEST] Rome Imperial Legacy',
        'Colosseum Vatican and ancient Roman ruins', 1900.00, 'CULTURAL', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-06-05', '2026-06-12', true, 10, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000002-0000-0000-0000-000000000003', '[TEST] Lviv Heritage Walk',
        'Discover the cultural capital of Western Ukraine', 600.00, 'CULTURAL', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-06-20', '2026-06-25', true, 20, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000003-0000-0000-0000-000000000001', '[TEST] Carpathian Eco Retreat',
        'Hiking and nature in the Ukrainian Carpathians', 800.00, 'ECO', 'BUS', 'THREE_STARS', 'AVAILABLE',
        '2026-05-15', '2026-05-22', true, 15, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000003-0000-0000-0000-000000000002', '[TEST] Norwegian Fjords', 'Sustainable travel through majestic fjords',
        2800.00, 'ECO', 'SHIP', 'FOUR_STARS', 'AVAILABLE', '2026-08-01', '2026-08-10', false, 10, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000004-0000-0000-0000-000000000001', '[TEST] Kenya Masai Mara Safari',
        'Wildlife safari through Masai Mara and Amboseli', 3500.00, 'SAFARI', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-08-01', '2026-08-10', false, 0, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000004-0000-0000-0000-000000000002', '[TEST] Tanzania Serengeti Migration',
        'Witness the great wildebeest migration', 4200.00, 'SAFARI', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-07-15',
        '2026-07-25', true, 10, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000005-0000-0000-0000-000000000001', '[TEST] Tuscany Wine Experience',
        'Visit vineyards and taste the finest Italian wines', 1800.00, 'WINE', 'TRAIN', 'FIVE_STARS', 'AVAILABLE',
        '2026-09-05', '2026-09-12', false, 5, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000005-0000-0000-0000-000000000002', '[TEST] Bordeaux Grand Cru Tour',
        'Explore legendary Bordeaux wine chateaux', 2500.00, 'WINE', 'TRAIN', 'FIVE_STARS', 'AVAILABLE', '2026-10-01',
        '2026-10-08', true, 10, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000006-0000-0000-0000-000000000001', '[TEST] Bali Wellness Retreat',
        'Yoga meditation and spa in tropical Bali', 2000.00, 'HEALTH', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-06-01',
        '2026-06-10', true, 10, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000006-0000-0000-0000-000000000002', '[TEST] Truskavets Spa',
        'Traditional Ukrainian mineral water spa resort', 500.00, 'HEALTH', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-05-10', '2026-05-20', false, 20, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000007-0000-0000-0000-000000000001', '[TEST] Alps Skiing Package',
        'Ski resorts in the French and Swiss Alps', 2500.00, 'SPORTS', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-12-15',
        '2026-12-25', true, 10, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000007-0000-0000-0000-000000000002', '[TEST] Bukovel Winter Sports',
        'Skiing and snowboarding in Ukrainian Bukovel', 700.00, 'SPORTS', 'BUS', 'THREE_STARS', 'AVAILABLE',
        '2026-12-20', '2026-12-28', true, 25, 10);

INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000008-0000-0000-0000-000000000001', '[TEST] Greek Islands Hopping', 'Santorini Mykonos and Crete by ferry',
        2200.00, 'LEISURE', 'SHIP', 'FOUR_STARS', 'AVAILABLE', '2026-07-15', '2026-07-25', true, 10, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000008-0000-0000-0000-000000000002', '[TEST] Odesa Black Sea Resort',
        'Sun sea and nightlife on the Black Sea coast', 400.00, 'LEISURE', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-07-01', '2026-07-08', true, 15, 10);
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000008-0000-0000-0000-000000000003', '[TEST] Maldives Beach Paradise',
        'Relax on pristine white sand beaches', 4000.00, 'LEISURE', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-06-01',
        '2026-06-10', false, 0, 10);
