# Travel Agency — Presentation Plan (15-20 min)

## Before Presentation
- Start Docker Desktop
- Run `docker-compose up -d`
- Open IDE with project
- Set `application.yml` active profile to `test`
- Start the app
- Open two browser tabs: one for user, one for admin
- Open `https://localhost:8443` (accept certificate warning)

---

## 1. Quick Overview (1 min)
- Show the home page
- "Full-stack Spring Boot 3.4 app with Thymeleaf"
- "3 roles: User, Manager, Admin"
- "Booking system with quantity, discounts, balance"
- "92 tests, 2 DB profiles, i18n, HTTPS, OAuth2, REST API"

---

## 2. Profile Switch Demo (2 min)
- Show `application.yml` — `active: test`
- Show catalogue — tours have [TEST] prefix, H2 database
- Open `application.yml` → change to `active: prod`
- Restart app
- Show catalogue — production data (44 tours, no [TEST] prefix), PostgreSQL
- "Same code, different data, different database — just one line change"
- Switch back to `test` for remaining demo

---

## 3. User Flow (3 min)

### Registration
- Click Register
- Submit empty form → show validation errors highlighted per field
- Enter weak password → show custom @StrongPassword validation
- Fill correctly → register → redirect to login

### Login & Browse
- Login with the new user
- Show catalogue — cards with HOT badge, discount badge, spots left
- Use filters: select Tour Type + Hotel Type simultaneously
- Change sort to "Price: Low to High"
- Show pagination

### Order & Cancel
- Click a voucher → show detail page with price, dates, quantity
- Click Order → redirected to My Vouchers
- Show booking with "Booked On" date, status REGISTERED
- Go back to catalogue → spots decreased
- Go to My Vouchers → Cancel the order
- Show: balance refunded, spots restored

---

## 4. Manager Flow (2 min)
- Login as `manager` / `Password1!`
- Go to Manager → Manage Vouchers
- Search by name — type "Egypt" → filtered
- Click Create New Voucher
- Fill the form — show date validation (eviction before arrival)
- Create successfully
- Go to Manager → Manage Orders
- Mark one booking as PAID
- Cancel another → "refund happens automatically"
- Show quantity can't go below active bookings (edit voucher, try to set quantity=0)

---

## 5. Admin Flow (2 min)
- Login as `admin` / `Password1!`
- Go to Admin → Manage Users
- Show paginated user list with roles and status
- Block a user → show success message
- Open another browser (incognito) → login as blocked user → navigate → gets kicked out immediately → "Account blocked" message
- Show: admin's own row has no action buttons — "can't modify yourself"
- Change a user's role to MANAGER → show dropdown
- Promote someone to ADMIN
- Go to Admin → Dashboard → show stats (users, bookings, revenue)

---

## 6. OAuth2 Google Login (1 min)
- Switch to prod profile (Google OAuth only works on prod)
- Restart app
- Go to login page → click "Login with Google"
- Google consent screen → approve
- Redirected back → logged in, navbar shows first name
- "OAuth2 creates a new user automatically with Google profile data"

---

## 7. REST API + Swagger (2 min)
- Open `https://localhost:8443/swagger-ui/index.html`
- Show two API groups: Voucher API (public) and Auth API (JWT)
- Execute GET /api/v1/vouchers → show JSON response with pagination
- Execute GET /api/v1/vouchers with tourType=ADVENTURE → filtered
- Execute POST /api/v1/auth/login with username/password → get JWT token
- Click "Authorize" button → paste token
- "This public API is for third-party integrations — ad platforms, partner sites, mobile apps"
- "Two security chains: sessions for web, JWT for API"

---

## 8. HTTPS (30 sec)
- Point at URL bar: `https://localhost:8443`
- "All traffic encrypted — passwords, sessions, tokens"
- "Self-signed certificate for development"
- "In production would use Let's Encrypt or commercial CA"

---

## 9. i18n Demo (1 min)
- Click language switcher → change to Ukrainian
- Show: navigation, buttons, tour types, statuses — all in Ukrainian
- Try to register with invalid data → show validation errors in Ukrainian
- "250 message keys in both English and Ukrainian"
- "Error messages from exceptions are also localized via MessageSource"

---

## 10. Code Highlights (3 min)

### Security (SecurityConfig.java)
- "Two filter chains with @Order"
- "API chain: stateless, JWT, CSRF disabled"
- "Web chain: sessions, form login, OAuth2, CSRF enabled"

### Dynamic Filtering (VoucherSpecification.java)
- "JPA Specifications — Criteria API"
- "6 filter criteria combinable dynamically"
- "One findAll(spec, pageable) replaces 64 method combinations"

### Blocked User Cache (BlockedUserFilter.java)
- "In-memory ConcurrentHashMap, refreshed every 30 seconds via @Scheduled"
- "Immediate eviction when admin blocks — no 30s delay"
- "Zero DB hits per request after first load"

### AOP Logging (LoggingAspect.java)
- "Cross-cutting concern — one class handles all logging"
- "DEBUG: method entry/exit, INFO: business events, WARN: security, ERROR: exceptions"
- "Never logs passwords"

### Testing
- "92 tests total: 63 service + 29 controller"
- Show @Nested structure in a test file
- Show @WithMockUser for security testing
- "Edge cases: 100% discount, race conditions, self-modification, expired tours"

---

## 11. Q&A
- Ready with PREPARATION.md (file-by-file guide) and QA.md (108 questions)
- Key topics to be prepared for:
  - Why session-based and not JWT for web?
  - Why separate Booking entity?
  - What about race conditions?
  - Why manual mappers?
  - How does OAuth2 flow work?
  - Why @EntityGraph?

---

## Tips
- Keep two browsers open (regular + incognito for blocked user demo)
- Have IDE open on the side for code highlights
- Start Docker BEFORE the presentation
- Use test profile for most demos, prod only for OAuth2
- If something breaks — the test profile with H2 always works without Docker
