-- Initial data for Travel Agency
-- Passwords are BCrypt encoded: admin123 -> $2a$10$7S4AQic4Vt/GMyfCIG5rB.7uKNYrmNGtkIwyA1aFJnK19Pr6z2ATW

-- Admin user
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('f3e02ce0-365d-4c03-90a1-98f00cf6d3d1', 'admin',
        '$2a$10$7S4AQic4Vt/GMyfCIG5rB.7uKNYrmNGtkIwyA1aFJnK19Pr6z2ATW',
        'ADMIN', 'Admin', 'User', 'admin@travelagency.com', '+380501234567', 0.00, true)
ON CONFLICT (id) DO NOTHING;

-- Manager user (password: admin123)
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'manager',
        '$2a$10$7S4AQic4Vt/GMyfCIG5rB.7uKNYrmNGtkIwyA1aFJnK19Pr6z2ATW',
        'MANAGER', 'Manager', 'User', 'manager@travelagency.com', '+380509876543', 0.00, true)
ON CONFLICT (id) DO NOTHING;

-- Sample user (password: admin123)
INSERT INTO "user" (id, username, password, role, first_name, last_name, email, phone_number, balance, account_status)
VALUES ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'user',
        '$2a$10$7S4AQic4Vt/GMyfCIG5rB.7uKNYrmNGtkIwyA1aFJnK19Pr6z2ATW',
        'USER', 'John', 'Doe', 'user@travelagency.com', '+380507654321', 5000.00, true)
ON CONFLICT (id) DO NOTHING;

-- ===================== VOUCHERS =====================

-- ADVENTURE tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000001-0000-0000-0000-000000000001', 'Egyptian Pyramid Adventure',
        'Explore the ancient pyramids of Giza and cruise the Nile', 1500.00, 'ADVENTURE', 'PLANE', 'FOUR_STARS',
        'AVAILABLE', '2026-06-01', '2026-06-14', true, 10, 10),
       ('c0000001-0000-0000-0000-000000000002', 'Amazon Jungle Expedition',
        'Deep into the Amazon rainforest with experienced guides', 2800.00, 'ADVENTURE', 'PLANE', 'TWO_STARS',
        'AVAILABLE', '2026-07-01', '2026-07-12', false, 0, 10),
       ('c0000001-0000-0000-0000-000000000003', 'Patagonia Trekking',
        'Hike through the stunning landscapes of Patagonia', 3200.00, 'ADVENTURE', 'PLANE', 'THREE_STARS', 'AVAILABLE',
        '2026-08-10', '2026-08-22', false, 5, 10),
       ('c0000001-0000-0000-0000-000000000004', 'Iceland Ring Road',
        'Drive around Iceland seeing geysers, waterfalls, and glaciers', 2500.00, 'ADVENTURE', 'PRIVATE_CAR',
        'THREE_STARS', 'AVAILABLE', '2026-06-15', '2026-06-25', true, 15, 10),
       ('c0000001-0000-0000-0000-000000000005', 'Nepal Himalaya Base Camp',
        'Trek to Everest Base Camp through Sherpa villages', 1800.00, 'ADVENTURE', 'PLANE', 'ONE_STAR', 'AVAILABLE',
        '2026-09-01', '2026-09-18', false, 0, 10)
ON CONFLICT (id) DO NOTHING;

-- CULTURAL tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000002-0000-0000-0000-000000000001', 'Paris Art and History',
        'Visit the Louvre, Eiffel Tower, and Versailles', 2200.00, 'CULTURAL', 'PLANE', 'FIVE_STARS', 'AVAILABLE',
        '2026-07-10', '2026-07-17', false, 0, 10),
       ('c0000002-0000-0000-0000-000000000002', 'Rome Imperial Legacy', 'Colosseum, Vatican, and ancient Roman ruins',
        1900.00, 'CULTURAL', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-06-05', '2026-06-12', true, 10, 10),
       ('c0000002-0000-0000-0000-000000000003', 'Kyoto Temples and Traditions',
        'Explore ancient temples, tea ceremonies, and geisha culture', 3000.00, 'CULTURAL', 'PLANE', 'FIVE_STARS',
        'AVAILABLE', '2026-10-01', '2026-10-10', false, 0, 10),
       ('c0000002-0000-0000-0000-000000000004', 'Athens Cradle of Civilization',
        'Acropolis, Parthenon, and Greek philosophy trail', 1600.00, 'CULTURAL', 'PLANE', 'THREE_STARS', 'AVAILABLE',
        '2026-05-20', '2026-05-27', false, 5, 10),
       ('c0000002-0000-0000-0000-000000000005', 'Istanbul East Meets West',
        'Hagia Sophia, Grand Bazaar, and Bosphorus cruise', 1400.00, 'CULTURAL', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-09-15', '2026-09-22', false, 0, 10),
       ('c0000002-0000-0000-0000-000000000006', 'Lviv Heritage Walk',
        'Discover the cultural capital of Western Ukraine', 600.00, 'CULTURAL', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-06-20', '2026-06-25', true, 20, 10)
ON CONFLICT (id) DO NOTHING;

-- ECO tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000003-0000-0000-0000-000000000001', 'Carpathian Eco Retreat',
        'Hiking and nature in the Ukrainian Carpathians', 800.00, 'ECO', 'BUS', 'THREE_STARS', 'AVAILABLE',
        '2026-05-15', '2026-05-22', true, 15, 10),
       ('c0000003-0000-0000-0000-000000000002', 'Costa Rica Rainforest',
        'Eco-lodges, zip-lining, and wildlife spotting', 2400.00, 'ECO', 'PLANE', 'THREE_STARS', 'AVAILABLE',
        '2026-07-05', '2026-07-15', false, 0, 10),
       ('c0000003-0000-0000-0000-000000000003', 'Norwegian Fjords', 'Sustainable travel through majestic fjords',
        2800.00, 'ECO', 'SHIP', 'FOUR_STARS', 'AVAILABLE', '2026-08-01', '2026-08-10', false, 10, 10),
       ('c0000003-0000-0000-0000-000000000004', 'Swiss Alps Green Tour',
        'Mountain hikes and eco-friendly alpine lodges', 2100.00, 'ECO', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-06-10', '2026-06-18', false, 0, 10),
       ('c0000003-0000-0000-0000-000000000005', 'New Zealand Nature Trail',
        'Explore pristine landscapes and Maori culture', 3500.00, 'ECO', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-11-01', '2026-11-14', false, 5, 10)
ON CONFLICT (id) DO NOTHING;

-- SAFARI tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000004-0000-0000-0000-000000000001', 'Kenya Masai Mara Safari',
        'Wildlife safari through Masai Mara and Amboseli', 3500.00, 'SAFARI', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-08-01', '2026-08-10', false, 0, 10),
       ('c0000004-0000-0000-0000-000000000002', 'Tanzania Serengeti Migration',
        'Witness the great wildebeest migration', 4200.00, 'SAFARI', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-07-15',
        '2026-07-25', true, 10, 10),
       ('c0000004-0000-0000-0000-000000000003', 'South Africa Big Five', 'Track the Big Five in Kruger National Park',
        3800.00, 'SAFARI', 'JEEPS', 'FOUR_STARS', 'AVAILABLE', '2026-09-01', '2026-09-10', false, 0, 10),
       ('c0000004-0000-0000-0000-000000000004', 'Botswana Okavango Delta', 'Explore the delta by mokoro canoe and jeep',
        4500.00, 'SAFARI', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-10-05', '2026-10-15', false, 5, 10),
       ('c0000004-0000-0000-0000-000000000005', 'Uganda Gorilla Trekking',
        'Trek through Bwindi to see mountain gorillas', 3000.00, 'SAFARI', 'JEEPS', 'TWO_STARS', 'AVAILABLE',
        '2026-06-20', '2026-06-28', false, 0, 10)
ON CONFLICT (id) DO NOTHING;

-- WINE tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000005-0000-0000-0000-000000000001', 'Tuscany Wine Experience',
        'Visit vineyards and taste the finest Italian wines', 1800.00, 'WINE', 'TRAIN', 'FIVE_STARS', 'AVAILABLE',
        '2026-09-05', '2026-09-12', false, 5, 10),
       ('c0000005-0000-0000-0000-000000000002', 'Bordeaux Grand Cru Tour', 'Explore legendary Bordeaux wine chateaux',
        2500.00, 'WINE', 'TRAIN', 'FIVE_STARS', 'AVAILABLE', '2026-10-01', '2026-10-08', true, 10, 10),
       ('c0000005-0000-0000-0000-000000000003', 'Napa Valley Tasting', 'California wine country at its finest', 2000.00,
        'WINE', 'PRIVATE_CAR', 'FOUR_STARS', 'AVAILABLE', '2026-08-15', '2026-08-22', false, 0, 10),
       ('c0000005-0000-0000-0000-000000000004', 'Georgian Wine Heritage',
        'Discover 8000-year-old winemaking tradition in Georgia', 900.00, 'WINE', 'MINIBUS', 'THREE_STARS', 'AVAILABLE',
        '2026-07-10', '2026-07-17', false, 15, 10),
       ('c0000005-0000-0000-0000-000000000005', 'Porto Wine Cellars', 'Explore the famous port wine cellars of Porto',
        1200.00, 'WINE', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-11-05', '2026-11-10', false, 0, 10)
ON CONFLICT (id) DO NOTHING;

-- HEALTH tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000006-0000-0000-0000-000000000001', 'Bali Wellness Retreat', 'Yoga, meditation, and spa in tropical Bali',
        2000.00, 'HEALTH', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-06-01', '2026-06-10', true, 10, 10),
       ('c0000006-0000-0000-0000-000000000002', 'Dead Sea Healing', 'Therapeutic mud baths and mineral-rich waters',
        1500.00, 'HEALTH', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-07-01', '2026-07-08', false, 0, 10),
       ('c0000006-0000-0000-0000-000000000003', 'Truskavets Spa', 'Traditional Ukrainian mineral water spa resort',
        500.00, 'HEALTH', 'TRAIN', 'THREE_STARS', 'AVAILABLE', '2026-05-10', '2026-05-20', false, 20, 10),
       ('c0000006-0000-0000-0000-000000000004', 'Thai Massage Retreat', 'Traditional Thai healing and wellness program',
        1800.00, 'HEALTH', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-08-05', '2026-08-12', false, 0, 10),
       ('c0000006-0000-0000-0000-000000000005', 'Iceland Hot Springs',
        'Geothermal pools and northern wellness experience', 2200.00, 'HEALTH', 'PLANE', 'FOUR_STARS', 'AVAILABLE',
        '2026-09-10', '2026-09-17', false, 5, 10)
ON CONFLICT (id) DO NOTHING;

-- SPORTS tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000007-0000-0000-0000-000000000001', 'Alps Skiing Package', 'Ski resorts in the French and Swiss Alps',
        2500.00, 'SPORTS', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-12-15', '2026-12-25', true, 10, 10),
       ('c0000007-0000-0000-0000-000000000002', 'Surfing in Portugal', 'Catch waves on the Atlantic coast', 1200.00,
        'SPORTS', 'PLANE', 'THREE_STARS', 'AVAILABLE', '2026-07-01', '2026-07-10', false, 0, 10),
       ('c0000007-0000-0000-0000-000000000003', 'Diving in Maldives', 'Explore coral reefs and marine life', 3800.00,
        'SPORTS', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-10-10', '2026-10-20', false, 5, 10),
       ('c0000007-0000-0000-0000-000000000004', 'Cycling Tour de France Route',
        'Ride through legendary Tour de France stages', 1600.00, 'SPORTS', 'TRAIN', 'TWO_STARS', 'AVAILABLE',
        '2026-06-25', '2026-07-05', false, 0, 10),
       ('c0000007-0000-0000-0000-000000000005', 'Bukovel Winter Sports', 'Skiing and snowboarding in Ukrainian Bukovel',
        700.00, 'SPORTS', 'BUS', 'THREE_STARS', 'AVAILABLE', '2026-12-20', '2026-12-28', true, 25, 10)
ON CONFLICT (id) DO NOTHING;

-- LEISURE tours
INSERT INTO voucher (id, title, description, price, tour_type, transfer_type, hotel_type, status, arrival_date,
                     eviction_date, is_hot, discount, quantity)
VALUES ('c0000008-0000-0000-0000-000000000001', 'Maldives Beach Paradise', 'Relax on pristine white sand beaches',
        4000.00, 'LEISURE', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-06-01', '2026-06-10', false, 0, 10),
       ('c0000008-0000-0000-0000-000000000002', 'Greek Islands Hopping', 'Santorini, Mykonos, and Crete by ferry',
        2200.00, 'LEISURE', 'SHIP', 'FOUR_STARS', 'AVAILABLE', '2026-07-15', '2026-07-25', true, 10, 10),
       ('c0000008-0000-0000-0000-000000000003', 'Caribbean Cruise', 'Seven-day cruise through the Caribbean islands',
        3500.00, 'LEISURE', 'SHIP', 'FIVE_STARS', 'AVAILABLE', '2026-08-01', '2026-08-08', false, 5, 10),
       ('c0000008-0000-0000-0000-000000000004', 'Zanzibar Beach Escape', 'Turquoise waters and spice island culture',
        1800.00, 'LEISURE', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-09-10', '2026-09-18', false, 0, 10),
       ('c0000008-0000-0000-0000-000000000005', 'Odesa Black Sea Resort',
        'Sun, sea, and nightlife on the Black Sea coast', 400.00, 'LEISURE', 'TRAIN', 'THREE_STARS', 'AVAILABLE',
        '2026-07-01', '2026-07-08', true, 15, 10),
       ('c0000008-0000-0000-0000-000000000006', 'Thailand Beach Holiday', 'Phuket and Koh Samui tropical relaxation',
        1600.00, 'LEISURE', 'PLANE', 'FOUR_STARS', 'AVAILABLE', '2026-11-01', '2026-11-10', false, 0, 10),
       ('c0000008-0000-0000-0000-000000000007', 'Bali Sunset Retreat', 'Luxury beachfront villas with infinity pools',
        2800.00, 'LEISURE', 'PLANE', 'FIVE_STARS', 'AVAILABLE', '2026-10-15', '2026-10-25', false, 10, 10),
       ('c0000008-0000-0000-0000-000000000008', 'Montenegro Coast', 'Explore the Adriatic coast and Bay of Kotor',
        1100.00, 'LEISURE', 'BUS', 'THREE_STARS', 'AVAILABLE', '2026-08-20', '2026-08-28', false, 0, 10)
ON CONFLICT (id) DO NOTHING;
