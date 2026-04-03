# Travel Agency — Q&A for Presentation

## 1. ARCHITECTURE & DESIGN

**Q: Describe the overall architecture of your application.**
A: It's a Spring Boot MVC application with Thymeleaf server-side rendering. Three-layer architecture: Controller (
handles HTTP requests) -> Service (business logic) -> Repository (data access). DTOs separate the view layer from
entities. Services are split by domain responsibility: Auth, User, UserManagement, Voucher, Order, Stats.

**Q: Why did you choose Thymeleaf over REST + React?**
A: Thymeleaf was listed as "nice to have" in requirements. Server-side rendering keeps everything in one project — no
separate frontend build, no CORS config, no JWT token management. Session-based auth with CSRF works naturally with
Thymeleaf forms.

**Q: Why 6 services instead of 2 (UserService + VoucherService)?**
A: Single Responsibility Principle. UserService handles profile operations, UserManagementService handles admin
operations on users, OrderService handles cross-entity transactions (booking involves both User balance and Voucher).
Each service has one reason to change.

**Q: Why does OrderService access repositories directly instead of through other services?**
A: Ordering is a cross-domain transaction — deducts user balance AND creates a booking atomically in one @Transactional
method. If we called UserService.deductBalance() + VoucherService.createBooking(), each has its own transaction — if the
second fails, the first already committed. Direct repository access keeps one transaction boundary.

**Q: What design patterns did you use?**
A: Builder (Lombok @Builder on entities/DTOs), Strategy (JPA Specifications for dynamic filtering), Observer (Spring
Security events for login logging), Proxy (Spring AOP for logging), Template Method (OncePerRequestFilter for
BlockedUserFilter), Factory Method (VoucherSpecification static factories), DTO pattern (request/response DTOs).

**Q: What is the anemic domain model and why did you choose it?**
A: Entities are pure data holders — no business logic. All logic lives in services. This is the standard Spring/JPA
approach. Entities are managed by Hibernate lifecycle — mixing business logic with persistence creates coupling.
Services are easier to unit test with Mockito.

**Q: How do you handle cross-cutting concerns?**
A: AOP (Aspect-Oriented Programming). LoggingAspect intercepts all service methods — logs entry/exit at DEBUG, business
events at INFO, errors at ERROR. This keeps logging code out of services. SecurityEventListener handles authentication
events.

---

## 2. SPRING SECURITY

**Q: Explain your security architecture.**
A: Three layers of protection: 1) URL-based (SecurityFilterChain — public/authenticated/MANAGER/ADMIN paths), 2)
Method-level (@PreAuthorize on services), 3) Custom filter (BlockedUserFilter checks user status per request).
Authentication uses DaoAuthenticationProvider with BCrypt and database-backed UserDetailsService.

**Q: Why session-based auth and not JWT?**
A: Thymeleaf renders HTML server-side — the browser needs a session, not a token. JWT is designed for stateless REST
APIs. With sessions, Spring Security handles CSRF protection automatically, and Thymeleaf includes CSRF tokens in forms
via `th:action`.

**Q: How does CSRF protection work in your app?**
A: Spring Security generates a CSRF token per session. Thymeleaf's `th:action` automatically includes it as a hidden
field in every POST form. If the token is missing or invalid, Spring rejects the request with 403. This prevents
malicious sites from submitting forms on behalf of the user.

**Q: What happens when a blocked user tries to navigate?**
A: BlockedUserFilter is a OncePerRequestFilter that checks if the authenticated user is blocked. It maintains an
in-memory Set of blocked usernames (ConcurrentHashMap.newKeySet() — thread-safe). On each request: gets the
Authentication from SecurityContext, checks if the username is in the blocked set. If blocked: clears SecurityContext,
invalidates session, redirects to /login?blocked=true. The login page shows a localized "account blocked" message.

**Q: How does the BlockedUserFilter cache work?**
A: Three mechanisms keep the cache fresh: 1) @Scheduled(fixedRate = 30000) — every 30 seconds, refreshBlockedUsers()
loads all blocked usernames from DB into a fresh Set, then swaps it into the cache. 2) Immediate eviction — when admin
blocks a user, UserManagementServiceImpl calls blockedUserFilter.evictUser(username) which instantly adds the username
to the blocked set (no 30s delay). 3) Constructor initialization — cache is populated on application startup via
refreshBlockedUsers() in the constructor.

**Q: Why in-memory cache instead of DB hit per request?**
A: Performance. Without cache: every HTTP request triggers a SELECT query — with 100 concurrent users that's 100
queries/second just for the filter. With cache: O(1) ConcurrentHashMap lookup per request, only 1 DB query every 30
seconds for refresh. Tradeoff: unblock takes up to 30 seconds to take effect (unless we also add an immediate unblock
cache update).

**Q: Why ConcurrentHashMap.newKeySet() and not HashSet?**
A: Thread safety. The filter runs in HTTP request threads while @Scheduled runs in a separate scheduler thread.
ConcurrentHashMap handles concurrent reads and writes without explicit synchronization. Regular HashSet would cause race
conditions — a thread reading while another writes could get corrupted data.

**Q: Why shouldNotFilter() skips certain paths?**
A: No point checking blocked status for public pages. Login, register, static resources (CSS/JS/images), error pages
don't require authentication — blocked check is irrelevant. Skipping them avoids unnecessary Set lookups and keeps the
filter fast.

**Q: What Spring feature does @Scheduled demonstrate?**
A: Spring Task Scheduling (@EnableScheduling on Application.java). It's listed as "Other Spring technologies" in the
project requirements. The scheduler runs refreshBlockedUsers() in a managed thread pool at a fixed 30-second interval,
independently of HTTP requests.

**Q: Why can't an admin modify their own account?**
A: Self-protection. If admin blocks themselves, they're locked out permanently.
UserManagementServiceImpl.validateNotSelf() compares target user's username with the current admin's username — throws
AccessDeniedException if they match.

**Q: How does role-based access control work at URL level vs method level?**
A: URL level: SecurityFilterChain defines paths — /manager/** requires MANAGER or ADMIN, /admin/** requires ADMIN.
Method level: @PreAuthorize("hasRole('ADMIN')") on UserManagementServiceImpl class. Both layers work together — even if
someone bypasses URL security, method security blocks the call.

**Q: How is the password stored and validated?**
A: BCryptPasswordEncoder hashes passwords with random salt. During registration, `passwordEncoder.encode(password)`
creates the hash. During login, Spring Security's DaoAuthenticationProvider calls
`passwordEncoder.matches(rawPassword, storedHash)`. The raw password is never stored.

**Q: What is your strong password policy?**
A: Custom @StrongPassword annotation with StrongPasswordValidator. Requirements: minimum 8 characters, at least one
uppercase letter, one lowercase letter, one digit, one special character. Uses precompiled regex Pattern objects for
performance.

---

## 3. DATABASE & JPA

**Q: Why ddl-auto=none instead of update or create?**
A: Full control over schema. Hibernate auto-generation doesn't create CHECK constraints (e.g., balance >= 0, discount
0-100), custom indexes, or complex constraints (eviction_date > arrival_date). schema.sql defines exactly what we need.

**Q: Explain your database schema.**
A: Three tables: "user" (quoted — reserved word), voucher, booking. User has profile fields + balance + role. Voucher
represents a tour product with quantity. Booking is a join table between User and Voucher — stores order status and
price snapshot. Foreign keys maintain referential integrity.

**Q: Why a separate Booking entity instead of user_id on Voucher?**
A: One voucher can be booked by multiple users (quantity support). If Voucher had user_id, only one user could book it.
Booking entity creates a many-to-many relationship with additional attributes (status, bookedPrice, createdAt).

**Q: What is bookedPrice and why do you store it?**
A: The price the user actually paid at booking time (after discount). If the manager changes the voucher price or
discount later, existing bookings and refunds should use the original price. This is standard e-commerce practice —
price snapshot at transaction time.

**Q: What is @EntityGraph and why do you use it?**
A: Tells JPA to eagerly fetch related entities using JOIN in a single SQL query. BookingMapper accesses
booking.getUser().getUsername() and booking.getVoucher().getTitle() — without @EntityGraph, each access triggers a
separate SELECT query (N+1 problem). With @EntityGraph, one query fetches booking + user + voucher.

**Q: Explain the N+1 problem and how you solved it.**
A: When loading a list of 10 bookings with LAZY fetch, accessing each booking's user triggers a separate query: 1 query
for bookings + 10 queries for users = 11 queries (N+1). Solution: @EntityGraph(attributePaths = {"user", "voucher"}) on
repository methods generates one JOIN query that fetches everything.

**Q: Why JPA Specifications instead of multiple repository methods?**
A: Dynamic filtering. Catalogue has 6 filter criteria (tourType, transferType, hotelType, minPrice, maxPrice, search).
Combining them would need 2^6 = 64 repository methods. JPA Specifications build criteria at runtime —
`spec.and(hasTourType("ADVENTURE")).and(priceBetween(100, 500))`. One `findAll(spec, pageable)` handles all
combinations.

**Q: What is JpaSpecificationExecutor and how does it work?**
A: It's a Spring Data JPA interface that adds Criteria API–based querying to any repository. When a repository extends
`JpaSpecificationExecutor<T>`, it gains methods like `findAll(Specification<T> spec, Pageable pageable)`,
`count(Specification<T> spec)`, and `findOne(Specification<T> spec)`. Spring Data auto-implements them at runtime — no
code to write. We use it on both `VoucherRepository` and `UserRepository`:
`extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher>`.

**Q: What is the Specification interface?**
A: `Specification<T>` is a functional interface with one method:
`Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)`. Root represents the entity (column
access via `root.get("fieldName")`), CriteriaBuilder creates predicates (equals, like, between, greaterThan), and
CriteriaQuery describes the overall query structure. Each Specification represents one reusable filter condition.

**Q: Walk through how a Specification is built and executed.**
A: Example — catalogue filtering: 1) Start with a base spec:
`Specification<Voucher> spec = VoucherSpecification.hasStatus(AVAILABLE).and(arrivalDateAfterToday())`. 2) Conditionally
chain more specs: `if (hasValue(tourType)) spec = spec.and(hasTourType(tourType))`. 3) Pass to repository:
`voucherRepository.findAll(spec, pageable)`. Spring Data translates this into a single SQL query with all conditions in
the WHERE clause using AND. The `.and()` and `.or()` methods are default methods on the Specification interface that
compose predicates.

**Q: How do you organize Specification classes?**
A: Each entity that needs dynamic filtering gets its own Specification class with static factory methods:
`VoucherSpecification` (hasStatus, hasTourType, hasTransferType, hasHotelType, priceBetween, titleContains,
arrivalDateAfterToday, arrivalDateBeforeOrEqualToday) and `UserSpecification` (usernameContains, hasRole, isActive). The
factory method pattern keeps the API clean: `VoucherSpecification.priceBetween(100, 500)` reads naturally and hides the
Criteria API complexity.

**Q: What SQL does a Specification chain generate?**
A: For example, `hasStatus(AVAILABLE).and(hasTourType("ADVENTURE")).and(priceBetween(100, 500))` generates:
`SELECT * FROM voucher WHERE status = 'AVAILABLE' AND tour_type = 'ADVENTURE' AND price BETWEEN 100 AND 500`. Each
`.and()` adds an AND condition. The Criteria API uses parameterized queries — values are bound as parameters, never
concatenated into SQL, preventing SQL injection.

**Q: How does priceBetween handle optional min/max?**
A: It's a three-branch conditional: if both minPrice and maxPrice are provided →
`cb.between(root.get("price"), min, max)`. If only minPrice → `cb.greaterThanOrEqualTo(...)`. If only maxPrice →
`cb.lessThanOrEqualTo(...)`. The calling code only adds this spec when at least one bound is present, so the null-null
case never occurs.

**Q: How does Specification compare to @Query or Querydsl?**
A: @Query is static — the JPQL is fixed at compile time, so you need a separate method for each filter combination.
Querydsl requires a plugin and annotation processor to generate Q-classes. JPA Specifications use the standard JPA
Criteria API (no extra dependencies), are composable at runtime (chain with .and()/.or()), and integrate directly with
Spring Data's repository pattern. For our use case — dynamic multi-criteria filtering — Specifications are the sweet
spot: more flexible than @Query, simpler than Querydsl.

**Q: Why is the Specification constructor private?**
A: Both VoucherSpecification and UserSpecification are utility classes — they only contain static factory methods. The
private constructor (`private VoucherSpecification() {}`) prevents instantiation, signaling that the class is a
namespace for static methods, not an object to be created. Same pattern as `java.util.Collections` or `java.lang.Math`.

**Q: How do you prevent SQL injection in the search feature?**
A: Two layers: 1) JPA Criteria API uses parameterized queries — user input is never concatenated into SQL. 2) We escape
LIKE wildcards (% and _) in VoucherSpecification.escapeLikeWildcards() so users can't manipulate the LIKE pattern.

**Q: What are Spring Profiles and how do you use them?**
A: Profiles activate different configurations per environment. Test profile: H2 in-memory database, test data
with [TEST] prefix. Prod profile: PostgreSQL via Docker, production data. Switch by changing `spring.profiles.active` in
application.yml. Each profile has its own application-{profile}.yml.

**Q: Why H2 for tests and PostgreSQL for production?**
A: H2 is in-memory — fast, zero setup, fresh DB on every start. Perfect for development and testing. PostgreSQL is the
production database — persistent, scalable, supports all SQL features. The schema.sql is compatible with both.

**Q: How does data initialization work?**
A: spring.sql.init.mode=always runs schema.sql then data.sql on every startup. schema.sql uses CREATE TABLE IF NOT
EXISTS — idempotent. Production data.sql uses ON CONFLICT (id) DO NOTHING (PostgreSQL) — won't duplicate. H2 data-h2.sql
uses plain INSERT into fresh in-memory DB.

**Q: What's the difference between data.sql, data-h2.sql, and data-test.sql?**
A: data.sql — production data for PostgreSQL (44 vouchers, ON CONFLICT syntax). data-h2.sql — development data for H2
test profile (21 vouchers with [TEST] prefix, plain INSERT). data-test.sql — predictable test data in
src/test/resources (5 users with known IDs, 6 vouchers, 2 bookings — used by integration tests).

---

## 4. BUSINESS LOGIC

**Q: Walk through the voucher ordering flow.**
A: 1) User clicks "Order" → CatalogueController.order() called. 2) OrderServiceImpl.orderVoucher() checks: voucher
status is AVAILABLE, arrival date is in future, active bookings < quantity, user balance >= discounted price. 3) Deducts
balance from user, saves user. 4) Creates Booking entity (status=REGISTERED, bookedPrice=discounted price). 5) Saves
booking. 6) Redirect to my-vouchers with success message. All in one @Transactional — if anything fails, everything
rolls back.

**Q: How does the discount calculation work?**
A: `discountedPrice = price × (100 - discount) / 100`. Uses BigDecimal for precision. Discount is an integer 0-100 (
percentage). Example: price=1000, discount=20 → 1000 × 80/100 = $800. This price is stored in Booking.bookedPrice.

**Q: What happens when a user cancels a booking?**
A: OrderServiceImpl.cancelOrder() checks: booking belongs to the user (ownership check), status is REGISTERED (can't
cancel PAID). Then: refunds bookedPrice to user balance, sets booking status to CANCELED. The voucher spot becomes
available (activeBookings count decreases).

**Q: What happens when a manager cancels a booking?**
A: Same refund logic. OrderServiceImpl.changeStatus() with CANCELED: refunds bookedPrice to user, sets status to
CANCELED. Manager can also mark as PAID — no refund, just status change.

**Q: How do you prevent ordering expired tours?**
A: OrderServiceImpl checks `voucher.getArrivalDate().isAfter(LocalDate.now())`. If the tour starts today or has already
started, throws InvalidOrderStatusException with localized "tour already started" message.

**Q: How does the quantity system work?**
A: Voucher has a `quantity` field (e.g., 10 spots). When ordering, we count active bookings (REGISTERED + PAID) via
`bookingRepository.countActiveBookingsByVoucherId()`. If count >= quantity, the tour is fully booked.
availableQuantity (displayed in UI) = quantity - activeBookings.

**Q: What happens if a manager sets quantity below active bookings?**
A: VoucherServiceImpl.update() checks: if new quantity < active bookings, throws InvalidOrderStatusException with
message "Quantity cannot be less than active bookings (X)". Prevents overselling.

**Q: Is there a race condition when two users order the last spot?**
A: Yes, potential race condition. Both users could pass the quantity check simultaneously. Solution: optimistic locking
with @Version on Voucher entity, or pessimistic locking with SELECT FOR UPDATE. Not implemented — acknowledged
limitation for this course project.

---

## 5. INTERNATIONALIZATION (i18n)

**Q: How does internationalization work in your app?**
A: MessageSource loads messages from messages_en.properties and messages_uk.properties. SessionLocaleResolver stores the
selected locale in the session. LocaleChangeInterceptor catches ?lang=en or ?lang=uk parameter. Thymeleaf uses #{key} to
resolve messages. Validation errors use {key} in annotations.

**Q: How are validation error messages internationalized?**
A: Validation annotations reference message keys: `@NotBlank(message = "{validation.user.username.required}")`.
WebConfig connects Jakarta Bean Validation to Spring's MessageSource via LocalValidatorFactoryBean. The key is resolved
from the current locale's properties file.

**Q: How are business error messages internationalized?**
A: Exceptions implement LocalizedException interface with getMessageKey() and getArgs(). Services throw exceptions with
message keys (e.g., "error.balance.insufficient"). Controllers and GlobalExceptionHandler resolve the key via
MessageSource.getMessage(key, args, locale).

**Q: Why LocalizedException interface instead of extending a base class?**
A: Preserves the existing exception hierarchy. InsufficientBalanceException extends RuntimeException,
UserNotFoundException extends ResourceNotFoundException. An interface adds i18n capability without changing the
inheritance chain.

**Q: What languages do you support?**
A: English (default) and Ukrainian. ~250 message keys each. Covers: validation errors, navigation, UI labels, business
errors, success messages, enum translations (tour types, hotel types, statuses, roles).

---

## 6. VALIDATION

**Q: What types of validation do you implement?**
A: Three types: 1) Standard Bean Validation (@NotBlank, @Size, @Email, @Pattern, @Min, @Max, @Future, @Positive). 2)
Custom validators (@StrongPassword — password complexity, @DateRange — class-level date comparison). 3) Business
validation in services (duplicate username, insufficient balance, quantity limits).

**Q: How are validation errors displayed in the UI?**
A: Thymeleaf checks `#fields.hasErrors('fieldName')` — adds Bootstrap's 'is-invalid' class to the input. Error message
displayed below in `<div class="invalid-feedback">` with `th:errors="*{fieldName}"`. All error messages are i18n keys
resolved from message bundles.

**Q: What's the difference between DTO validation and service validation?**
A: DTO validation (Bean Validation) checks format — is the field blank? Is the email valid? Is the password strong
enough? Service validation checks business rules — does the username already exist? Is the balance sufficient? Is the
tour still available? Both are needed.

**Q: Explain the @DateRange custom validator.**
A: Class-level annotation on VoucherCreateDTO. DateRangeValidator gets the whole DTO, checks if evictionDate >
arrivalDate. If invalid, adds the error to the "evictionDate" field specifically using
`context.buildConstraintViolationWithTemplate().addPropertyNode("evictionDate")`.

---

## 7. EXCEPTION HANDLING

**Q: Explain your exception hierarchy.**
A: ResourceNotFoundException is the parent for not-found errors (UserNotFoundException, VoucherNotFoundException,
BookingNotFoundException) — maps to 404. LocalizedException is an interface for errors with i18n support (
DuplicateUsernameException, InsufficientBalanceException, InvalidOrderStatusException) — maps to 400.
UserBlockedException — maps to 403.

**Q: How does GlobalExceptionHandler work?**
A: @ControllerAdvice catches exceptions globally. Each @ExceptionHandler method creates a ModelAndView with the error
template and localized message. For LocalizedException: resolves message via MessageSource. For generic Exception:
shows "unexpected error" with 500 status. No stack traces are ever shown to users.

**Q: Why @ControllerAdvice and not @RestControllerAdvice?**
A: We use Thymeleaf — need to return HTML views, not JSON. @ControllerAdvice returns ModelAndView objects that render
Thymeleaf templates. @RestControllerAdvice would return JSON response bodies.

**Q: How do you prevent stack trace exposure?**
A: Two layers: 1) application.yml has `server.error.include-stacktrace: never` and `include-message: never`. 2)
GlobalExceptionHandler catches all exceptions including generic Exception.class and returns user-friendly error pages.
Even if an unhandled exception slips through, Spring Boot's defaults won't expose the stack trace.

---

## 8. LOGGING

**Q: How is logging implemented?**
A: AOP-based with LoggingAspect. @Before on all service methods logs entry with arguments (DEBUG). @AfterReturning logs
exit (DEBUG). @AfterThrowing logs exceptions (ERROR). @Around on specific business methods logs events (INFO/WARN).
SecurityEventListener logs login success/failure.

**Q: Why AOP for logging instead of manual log statements?**
A: Cross-cutting concern — logging is the same pattern across all services. AOP keeps it in one place. Adding a new
service method automatically gets entry/exit logging without any code in the service itself. Follows the Open/Closed
Principle.

**Q: What log levels do you use and when?**
A: DEBUG: method entry/exit with parameters (development only). INFO: business events (order placed, user registered,
status changed). WARN: security-sensitive actions (user blocked, role changed, failed login). ERROR: exceptions in
service layer.

**Q: Do you log sensitive data?**
A: No. Registration logging explicitly extracts only username from RegisterDTO: `((RegisterDTO) args[0]).getUsername()`.
The full DTO (which contains password) is never logged. Passwords are never in log output.

**Q: How does logging differ between profiles?**
A: logback-spring.xml defines per-profile config. Test profile: DEBUG level, console only. Prod profile: INFO level,
console + rolling file (daily, 30 days retention). Default: INFO level, console only.

---

## 9. TESTING

**Q: What testing strategy do you use?**
A: Three levels: 1) Unit tests — Mockito mocks, test service business logic in isolation (63 tests). 2) Controller
tests — @WebMvcTest with MockMvc, test HTTP layer + security (29 tests). 3) Integration test — @SpringBootTest with real
H2, verifies full context loads (1 test). Total: 92 tests.

**Q: Why Mockito for service tests?**
A: Services depend on repositories, password encoders, mappers. Mocking these dependencies isolates the service logic.
Tests run fast (no DB), are deterministic (no external state), and test exactly one thing.

**Q: How do you test security in controllers?**
A: @WithMockUser(roles = "MANAGER") simulates an authenticated user with specific role. @WebMvcTest loads only the web
layer + security config. Tests verify: 403 for wrong role, redirect to login for unauthenticated, correct view returned
for authorized.

**Q: What's @Nested and @DisplayName?**
A: @Nested groups related tests into inner classes — organizes by method being tested. @DisplayName provides
human-readable test names in reports. Together they create a structured test report: "OrderServiceImpl > orderVoucher >
should throw when balance is insufficient".

**Q: What edge cases do you test?**
A: 100% discount (free tour), duplicate booking same voucher, quantity exhausted, past tour dates, self-modification by
admin, blank password update, zero balance top-up, cancel PAID booking (should fail), manager cancel with refund.

**Q: Why @MockitoBean instead of @MockBean?**
A: Spring Boot 3.4+ deprecated @MockBean in favor of @MockitoBean from spring-test module. Same functionality, different
package: `org.springframework.test.context.bean.override.mockito.MockitoBean`.

---

## 10. THYMELEAF & FRONTEND

**Q: How does the template structure work?**
A: fragments/layout.html defines reusable parts: head (CSS), navbar (with role-based menu), footer, scripts (Bootstrap
JS). Each page uses `th:replace` to include these fragments. This is the Thymeleaf equivalent of a master layout.

**Q: How do you show/hide elements based on user role?**
A: thymeleaf-extras-springsecurity6 provides `sec:authorize`. Example: `sec:authorize="hasRole('ADMIN')"` shows admin
menu. `sec:authorize="isAuthenticated()"` shows logout button. `sec:authorize="!isAuthenticated()"` shows login/register
links.

**Q: How does the collapsible filter work without JavaScript?**
A: Bootstrap's Collapse component. A button with `data-bs-toggle="collapse" data-bs-target="#filterPanel"` toggles
visibility of the filter div. Bootstrap JS (included via CDN) handles the animation. No custom JavaScript needed.

**Q: How is pagination implemented?**
A: Services return `Page<DTO>` (Spring Data's paginated result). Templates render page numbers using
`th:each="i : ${#numbers.sequence(0, vouchers.totalPages - 1)}"`. Pagination links include all current filter parameters
to preserve state across pages.

**Q: How are validation errors shown in forms?**
A: `th:classappend="${#fields.hasErrors('username')} ? 'is-invalid'"` adds Bootstrap's error class.
`<div class="invalid-feedback" th:errors="*{username}">` shows the localized error message below the field. Bootstrap
styles make the field red with error text.

---

## 11. DOCKER & DEPLOYMENT

**Q: Why Docker for the database?**
A: Reproducible environment. `docker-compose up -d` starts PostgreSQL with exact version and config. No need to install
PostgreSQL locally. Team members get identical setup.

**Q: How do you handle the timezone issue?**
A: PostgreSQL 16 renamed timezone "Europe/Kiev" to "Europe/Kyiv". Java's JVM on Ukrainian systems sends "Europe/Kiev" —
PostgreSQL rejects it. Fix: `TimeZone.setDefault(UTC)` in Application.main() before Spring context starts. Docker also
sets TZ=UTC.

**Q: How does Spring Boot load .env file?**
A: `spring.config.import: optional:file:.env[.properties]` in application-prod.yml. Spring Boot reads the .env file as a
properties file. Variables like DB_USERNAME=travel become available as ${DB_USERNAME} in YAML. The `optional:` prefix
means it won't fail if .env doesn't exist.

---

## 12. CODE QUALITY & PRINCIPLES

**Q: How do you follow DRY principle?**
A: Shared constants for repeated strings. Helper methods like findVoucherById() reused across methods. VoucherMapper
handles all DTO-entity mapping. GlobalExceptionHandler.buildErrorView() for all error responses. LoggingAspect handles
all logging in one place.

**Q: How do you follow KISS principle?**
A: Manual mappers instead of MapStruct (simpler for 3 mappers). blockUser/unblockUser as separate methods (more readable
than setStatus(boolean)). No unnecessary abstractions — each class has a clear purpose.

**Q: How do you follow SOLID?**
A: Single Responsibility: each service has one domain. Open/Closed: JPA Specifications — add new filter without
modifying existing code. Liskov Substitution: UserNotFoundException extends ResourceNotFoundException — handled the same
way. Interface Segregation: small service interfaces. Dependency Inversion: services depend on repository interfaces,
controllers depend on service interfaces.

**Q: Why do you extract string literals to constants?**
A: Prevents typos (compiler catches wrong constant name, not wrong string). Enables reuse (same message key used in
service + test). Satisfies Sonar rules. Makes refactoring safer — change in one place.

**Q: Why not use Swagger/OpenAPI?**
A: No REST endpoints. The app uses Thymeleaf server-side rendering — browsers interact with HTML pages, not API
endpoints. Swagger documents REST APIs — irrelevant for MVC + Thymeleaf.

---

## 13. REST API + JWT + SWAGGER

**Q: Why do you have both MVC controllers and REST controllers?**
A: Different consumers. MVC controllers serve Thymeleaf HTML pages for browser users. REST API (`/api/v1/`) serves JSON
for third-party integrations — ad platforms, mobile apps, partner sites that want to display our tours. Same services,
different presentation layer.

**Q: How does the dual security architecture work?**
A: Two SecurityFilterChains with @Order. Order(1): API chain — matches `/api/**`, stateless (no session), CSRF disabled,
JWT authentication via JwtAuthenticationFilter. Order(2): Web chain — matches everything else, session-based, CSRF
enabled, form login + OAuth2. Each chain is independent — API requests never touch session logic, web requests never
touch JWT.

**Q: How does JWT authentication work?**
A: Flow: 1) Client sends POST `/api/v1/auth/login` with username/password JSON. 2) ApiAuthController authenticates via
AuthenticationManager. 3) JwtService generates a signed JWT token (HS256, 24h expiry) containing username and role. 4)
Client receives `{"token": "eyJ..."}`. 5) For subsequent requests, client sends `Authorization: Bearer eyJ...` header.
6) JwtAuthenticationFilter extracts token, validates signature and expiry, loads UserDetails, sets SecurityContext. All
API requests are stateless — no session, no cookies.

**Q: Why CSRF disabled for API but enabled for web?**
A: CSRF protects against cross-site form submissions — relevant for browser sessions with cookies. REST API uses JWT in
Authorization header — no cookies, no CSRF risk. Disabling CSRF for API is standard practice. Web forms still need CSRF
because browsers automatically send session cookies.

**Q: How does Swagger work?**
A: springdoc-openapi scans @RestController classes and generates OpenAPI 3.0 specification automatically. Swagger UI
renders it at `/swagger-ui.html`. SwaggerConfig adds Bearer Authentication scheme — the "Authorize" button in Swagger UI
lets you paste a JWT token for testing protected endpoints. Annotations like @Operation and @Parameter add descriptions.

**Q: What's the JWT token structure?**
A: Three parts (header.payload.signature). Header: algorithm (HS256). Payload: subject (username), role, issued-at,
expiration (24h). Signature: HMAC-SHA256 with secret key from environment variable JWT_SECRET. The secret key is in
.env (gitignored), with a default fallback for development.

---

## 14. OAUTH2 (GOOGLE LOGIN)

**Q: Explain the complete OAuth2 login flow.**
A: Step by step: 1) User clicks "Login with Google" button on login page. 2) Browser redirects to
`https://localhost:8443/oauth2/authorization/google`. 3) Spring Security's OAuth2 client redirects to Google's
authorization URL with our client_id, redirect_uri, and requested scopes (email, profile). 4) Google shows consent
screen — user approves. 5) Google redirects back to `https://localhost:8443/login/oauth2/code/google?code=...` with an
authorization code. 6) Spring Security exchanges the code for an access token with Google's token endpoint (
server-to-server, user never sees this). 7) Spring calls Google's userinfo endpoint with the access token to get user
profile (email, name). 8) Our OAuth2UserService.loadUser() is called with the Google user data. 9) We check if a user
with this Google ID exists in our database — if not, we create one (username=googleId, email from Google, random
password). 10) OAuth2User is returned, Spring creates a session, user is redirected to /catalogue. The entire flow takes
about 2 seconds.

**Q: Why store the Google numeric ID as username instead of email?**
A: Because `Principal.getName()` returns the Google user ID (numeric), not the email. All our controllers use
`principal.getName()` to identify the current user. If we stored email as username, the lookup would fail because
Principal returns the ID. Storing Google ID as username ensures consistency across all controllers.

**Q: Why generate a random password for OAuth2 users?**
A: The User entity requires a non-null password (database constraint). OAuth2 users don't use passwords — they
authenticate via Google. The random UUID password is never used for login — it's just to satisfy the schema. BCrypt
encoding ensures it's securely stored even though it's meaningless.

**Q: How does OAuth2 coexist with form login?**
A: SecurityConfig's web chain has both `.formLogin()` and `.oauth2Login()` configured. The login page shows both
options — username/password form and "Login with Google" button. Both create the same type of session. After login (
either method), the user is redirected to /catalogue. Controllers use `Principal` interface which works with both
authentication types.

**Q: Where are Google credentials stored?**
A: In `.env` file (gitignored): GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET. application-prod.yml references them as
`${GOOGLE_CLIENT_ID}` and `${GOOGLE_CLIENT_SECRET}`. Test profile has dummy values (`disabled`) — Google login only
works on prod profile.

**Q: What happens if Google login fails?**
A: Spring Security handles errors. Invalid credentials: Google shows error on their consent screen. Network error:
Spring redirects to /login?error. Our app never handles raw Google tokens — Spring's OAuth2 client library manages the
entire exchange securely.

**Q: What scopes do you request from Google?**
A: `email` and `profile`. Email gives us the user's email address. Profile gives first name and last name. We don't
request any other permissions — minimal data principle. No access to contacts, calendar, or other Google services.

**Q: Can the OAuth2 user access admin/manager features?**
A: No. OAuth2 users are created with Role.USER. Only an existing admin can promote them to MANAGER or ADMIN via the
admin panel. This prevents privilege escalation through OAuth2.

---

## 15. HTTPS

**Q: Why HTTPS?**
A: Encrypts all traffic between browser and server. Without HTTPS: passwords, session cookies, JWT tokens, and personal
data travel in plaintext — anyone on the network can intercept them. Required for OAuth2 (Google rejects HTTP redirect
URIs in production). Listed as optional security best practice in requirements.

**Q: How is HTTPS configured?**
A: Self-signed PKCS12 certificate generated with Java's `keytool`. Stored in `keystore.p12` (gitignored).
application.yml configures: `server.port=8443`, `server.ssl.key-store=classpath:keystore.p12`, password from environment
variable. Server only accepts HTTPS — no HTTP port.

**Q: Why self-signed certificate?**
A: For development and course project. Real CA certificates (Let's Encrypt) require a public domain name. Self-signed
works on localhost but browsers show a warning ("not secure") — user must click "Advanced → Proceed". In production,
we'd use Let's Encrypt or a commercial CA certificate.

**Q: Why port 8443 and not 443?**
A: Port 443 is the standard HTTPS port but requires root/admin privileges on Unix systems. Port 8443 is a convention for
development HTTPS — works without special permissions.

---

## 16. GLOBAL MODEL ADVICE + PRINCIPAL

**Q: Why did you switch from @AuthenticationPrincipal UserDetails to java.security.Principal?**
A: OAuth2 login returns OAuth2User, not UserDetails. Using `@AuthenticationPrincipal UserDetails` would cause
ClassCastException for Google-authenticated users. `java.security.Principal` is the common interface — both UserDetails
and OAuth2User implement it. `principal.getName()` works for both: returns username for form login, Google ID for
OAuth2.

**Q: What is GlobalModelAdvice and why do you need it?**
A: It's a @ControllerAdvice that adds `displayName` attribute to every request's model. The navbar needs to show the
user's name, but `Principal.getName()` returns a Google numeric ID for OAuth2 users (e.g., "115464081552235759214").
GlobalModelAdvice looks up the User entity by username (or email as fallback), extracts firstName, and provides it as a
model attribute. Thymeleaf template uses `${displayName}` instead of `sec:authentication="name"`.

**Q: Why does GlobalModelAdvice look up by username OR email?**
A: Defense in depth. Form login users have a readable username — lookup by username works. OAuth2 users have Google ID
as username — lookup by username also works. The email fallback handles edge cases where the principal name might be the
email (different OAuth2 providers return different identifiers). The `.or()` chain tries username first, then email,
then falls back to the raw principal name.

**Q: How do you avoid hitting the DB on every request for the display name?**
A: Session caching. First request after login: GlobalModelAdvice checks `session.getAttribute("displayName")` — null, so
it queries DB, resolves the name, stores it in session via `session.setAttribute()`. All subsequent requests: reads from
session, zero DB queries. Session is cleared automatically on logout. This is a simple and effective caching strategy
without external tools like Redis.

---

## 17. FUTURE EXPANSION

**Q: How would you expand this application?**
A: Near-term: email notifications (Spring Mail for booking confirmations), voucher images (imageUrl field), password
reset (token-based with @Scheduled cleanup), reviews/ratings (new entity, only PAID users can review), wishlist (
many-to-many User-Voucher). Medium-term: OAuth2 login (Google/Facebook via spring-boot-starter-oauth2-client), REST API
alongside Thymeleaf (for mobile app, reuse same services), payment gateway (Stripe/PayPal instead of balance).
Long-term: microservices split, Redis caching, async processing.

**Q: How would you add a REST API without breaking the existing app?**
A: Add `/api/v1/**` REST controllers alongside existing MVC controllers. Both call the same services — business logic is
reused. Add JWT authentication for API (stateless), keep session auth for web. SecurityConfig gets two filter chains —
one for `/api/**` (JWT), one for everything else (form login). DTOs are already separate from entities — same response
DTOs work for JSON.

**Q: How would you add OAuth2 (Google login)?**
A: Add spring-boot-starter-oauth2-client. Configure Google client ID/secret in application.yml. Add "Login with Google"
button on login page. OAuth2 callback receives user info → calls UserService.findOrCreateByEmail() → creates session.
Works because AuthService already delegates to UserService for user creation — single entry point.

**Q: How would you improve performance?**
A: 1) Redis cache for voucher catalogue (most read, rarely changes). 2) Cache blocked user status in BlockedUserFilter (
TTL 30 seconds instead of DB hit per request). 3) Database connection pooling tuning (HikariCP config). 4) Elasticsearch
for full-text search instead of SQL LIKE. 5) CDN for static assets (CSS, Bootstrap).

**Q: How would you handle the race condition in ordering?**
A: Option A: Optimistic locking — add @Version field to Voucher. If two users order simultaneously, second one gets
OptimisticLockException, retry or show "sold out". Option B: Pessimistic locking — `SELECT FOR UPDATE` in repository
query locks the row during transaction. Option A is preferred for low-contention scenarios (most tours have many spots).

**Q: Why does the architecture support easy expansion?**
A: Five reasons: 1) Services behind interfaces — swap implementation without touching controllers. 2) DTOs separate view
from entities — add API layer without changing services. 3) JPA Specifications — add new filters without modifying
existing code. 4) i18n infrastructure — add new language by adding one properties file. 5) Spring Profiles — add
staging/production-eu profiles without code changes.

---

## 18. EMAIL VALIDATION

**Q: Why do you use @Email with a custom regexp instead of just @Email?**
A: Jakarta's default @Email is very permissive — it accepts strings like "test" without an @ symbol as valid. Adding
`regexp = ".+@.+\\..+"` enforces that the email must contain at least `something@something.something`. This is a
stricter but still practical check — catches obvious invalid inputs without rejecting unusual but valid emails.

**Q: Why is email required on registration but optional on profile update?**
A: Registration needs a valid email for the account (required by business rules — contacts, potential notifications).
Profile update uses the same @Email regexp but without @NotBlank — user can leave it empty if they don't want to change
it. The service only updates non-blank fields.

---

## 19. PRICE RANGE VALIDATION

**Q: How do you handle invalid price range in filtering?**
A: Three layers: 1) HTML `min="0"` on inputs prevents negative values in the browser. 2) Service checks if minPrice >
maxPrice — throws InvalidOrderStatusException with localized message "Min price cannot be greater than max price". 3)
CatalogueController catches the exception, shows empty results with error alert. The user sees a clear error message
instead of confusing results.

**Q: Why throw an exception instead of silently swapping min and max?**
A: Better UX. Silent swapping hides the user's mistake — they might not notice the filter is different from what they
entered. Showing an explicit error teaches the user what went wrong and lets them correct it intentionally.

---

## 20. MANAGER CANCEL PAID BOOKINGS

**Q: Can a manager cancel a PAID booking?**
A: Yes. Managers can cancel both REGISTERED and PAID bookings. In both cases, the user receives a full refund of the
bookedPrice. Only already CANCELED bookings cannot be changed. This reflects real travel agency workflow — a paid tour
can still be canceled with refund (e.g., force majeure, customer request).

**Q: How does the cancel flow differ for REGISTERED vs PAID?**
A: The backend logic is identical — refund bookedPrice to user balance, set status to CANCELED. The difference is only
in the UI: REGISTERED bookings show both "Paid" and "Cancel" buttons, PAID bookings show only the "Cancel" button (
outline style to indicate it's a reversal action).

**Q: What prevents canceling an already canceled booking?**
A: Service checks `booking.getStatus() == BookingStatus.CANCELED` — if so, throws InvalidOrderStatusException. The
template also hides action buttons for CANCELED bookings, showing "-" instead. Double protection: UI + backend.

---

## 21. FORM VALIDATION DISPLAY

**Q: Why do you use novalidate on forms?**
A: HTML5 browsers have built-in validation (e.g., type="email" shows "Please include an @"). This runs before the form
reaches the server, bypassing our custom i18n validation messages. Adding `novalidate` disables browser validation, so
all validation is handled server-side with our @StrongPassword, @DateRange, and localized error messages displayed via
Thymeleaf's `#fields.hasErrors()`.

**Q: How are validation errors displayed to the user?**
A: Three parts working together: 1) `th:classappend="${#fields.hasErrors('field')} ? 'is-invalid'"` adds Bootstrap's red
border to the invalid input. 2) `<div class="invalid-feedback" th:errors="*{field}">` shows the localized error message
below the field. 3) Messages come from `messages_en.properties` or `messages_uk.properties` depending on the current
locale. All validation annotations use `message = "{key}"` syntax that MessageSource resolves.

---

## 22. EXPIRED VOUCHER FILTERING

**Q: How do you handle expired vouchers in the catalogue?**
A: The catalogue automatically hides expired tours. VoucherServiceImpl.findFiltered() builds a JPA Specification with
`VoucherSpecification.arrivalDateAfterToday()` — only vouchers whose arrival date is strictly after today are shown.
Users never see past tours they can't book.

**Q: How is the arrivalDateAfterToday specification implemented?**
A: `(root, query, cb) -> cb.greaterThan(root.get("arrivalDate"), LocalDate.now())`. It generates SQL:
`WHERE arrival_date > CURRENT_DATE`. The companion `arrivalDateBeforeOrEqualToday()` uses `cb.lessThanOrEqualTo` for the
expired filter. Both are static factory methods in VoucherSpecification, consistent with the existing filter pattern.

**Q: Can managers still see expired vouchers?**
A: Yes. Managers use a separate method `findAllForManager()` that does NOT filter by date by default. Instead, managers
have multiple filters: date dropdown (All/Active/Expired), tour type dropdown (all 8 types), and title search. All
filters are combinable — e.g., show only expired Adventure tours matching "safari". This lets managers review past
tours, update them, or archive them.

**Q: Why separate findFiltered() and findAllForManager() methods?**
A: Different audiences, different needs. Catalogue users should only see bookable tours — expired tours would be
confusing and unorderable. Managers need the full picture — they manage the entire lifecycle including past tours.
Mixing this logic into one method with flags would violate Single Responsibility Principle.

**Q: How does the manager date filter preserve state across pagination?**
A: The `dateFilter` parameter is included in pagination links:
`th:href="@{/manager/vouchers(page=${i}, search=${search}, dateFilter=${dateFilter})}"`. Same pattern used for the
search parameter. Both are passed as hidden fields or URL parameters so the user's filter choice persists when
navigating pages.

---

## 23. VOUCHER DELETION PROTECTION

**Q: Can a manager delete a voucher that has active bookings?**
A: No. VoucherServiceImpl.delete() checks `bookingRepository.countActiveBookingsByVoucherId(id)`. If count > 0, throws
InvalidOrderStatusException with localized message "Cannot delete voucher with X active booking(s)". The manager sees an
error alert on the voucher list page.

**Q: What counts as an "active" booking?**
A: REGISTERED or PAID status. The repository query `countActiveBookingsByVoucherId` counts bookings where status is not
CANCELED. These represent real obligations — the user either expects the tour or has already paid. CANCELED bookings
don't count because the user was already refunded.

**Q: What should the manager do to delete a voucher with active bookings?**
A: Two options: 1) Cancel all active bookings first (each gets a refund), then delete. 2) Set the voucher status to
DISABLED — it won't appear in the catalogue but remains in the system for record-keeping. Option 2 is preferred for
auditing purposes.

**Q: Why not cascade-delete bookings when a voucher is deleted?**
A: Data integrity. Bookings represent financial transactions — a user paid money and received a refund. Deleting booking
records would lose the audit trail. The deletion check forces the manager to explicitly handle each booking (cancel with
refund) before removing the voucher.

---

## 24. ADMIN USER MANAGEMENT FILTERING

**Q: How does user search and filtering work in the admin panel?**
A: Admin panel uses the same JPA Specification pattern as the catalogue. UserManagementServiceImpl.getAllUsers() accepts
three optional filters: `search` (username substring match), `role` (USER/MANAGER/ADMIN), `status` (active/blocked). It
builds a `Specification<User>` dynamically — starts with `Specification.where(null)` (matches everything), then
conditionally chains `.and(usernameContains(...))`, `.and(hasRole(...))`, `.and(isActive(...))`. All filters are
combinable — e.g., search "john" + role MANAGER + status active works in a single query.

**Q: How does the username search work internally?**
A: `UserSpecification.usernameContains(search)` generates a case-insensitive LIKE query:
`cb.like(cb.lower(root.get("username")), "%" + escaped + "%")`. The input is lowercased and LIKE wildcards (% and _) are
escaped via `escapeLikeWildcards()` — same approach as VoucherSpecification.titleContains(). This prevents users from
injecting wildcards into the search pattern.

**Q: Why Specification.where(null) as the base?**
A: `Specification.where(null)` returns a Specification that produces a null Predicate — JPA interprets this as "no
condition" (match all rows). It's a clean starting point for conditional chaining: `spec = spec.and(...)` works
correctly whether zero, one, or all filters are applied. Without it, you'd need an `if/else` ladder to decide the first
spec.

**Q: How are filter parameters preserved across pagination?**
A: All three filter params (`search`, `roleFilter`, `statusFilter`) are included in pagination URLs:
`th:href="@{/admin/users(page=${i}, search=${search}, roleFilter=${roleFilter}, statusFilter=${statusFilter})}"`. The
controller passes them as model attributes, and the template renders them into the filter form (as input values and
selected options) and pagination links. Clicking page 2 with filters active keeps the filters.

---

## 25. MANAGER ORDER FILTERING

**Q: How does order search and filtering work for managers?**
A: Same JPA Specification pattern used across the app. OrderServiceImpl.getAllOrders() accepts three optional filters:
`search` (voucher title substring), `username` (user substring), `status` (REGISTERED/PAID/CANCELED). It builds
`Specification<Booking>` dynamically — starts with `Specification.where(null)`, conditionally chains
`.and(voucherTitleContains(...))`, `.and(usernameContains(...))`, `.and(hasStatus(...))`. All filters are combinable in
a single query.

**Q: How does BookingSpecification search across related entities?**
A: Booking has ManyToOne relationships to User and Voucher. BookingSpecification uses JPA path navigation to search
across joins: `root.get("voucher").get("title")` for voucher title, `root.get("user").get("username")` for username. JPA
Criteria API translates this into SQL JOINs automatically —
`JOIN voucher v ON b.voucher_id = v.id WHERE LOWER(v.title) LIKE ...`. No manual JOIN code needed.

**Q: How do you prevent N+1 queries when using Specifications on Booking?**
A: By default, `JpaSpecificationExecutor.findAll(Specification, Pageable)` doesn't apply `@EntityGraph` annotations —
those only apply to named repository methods. We explicitly override the method in BookingRepository:
`@EntityGraph(attributePaths = {"user", "voucher"}) Page<Booking> findAll(Specification<Booking> spec, Pageable pageable)`.
This ensures one JOIN query even when filtering with Specifications, preventing N+1 when BookingMapper accesses
`booking.getUser().getUsername()` and `booking.getVoucher().getTitle()`.

**Q: Why three Specification classes instead of one generic class?**
A: Each class is entity-specific: VoucherSpecification (voucher filters — tourType, hotelType, price, date),
UserSpecification (user filters — username, role, active status), BookingSpecification (booking filters — voucher title,
username, booking status). Keeping them separate follows Single Responsibility — each class knows only its entity's
fields and relationships. A generic approach would need reflection or type casting, adding complexity for no benefit.

**Q: How does the status filter dropdown work in the template?**
A: The controller passes `BookingStatus.values()` as a model attribute. Thymeleaf iterates over it:
`th:each="s : ${bookingStatuses}"` renders an `<option>` for each enum value.
`th:selected="${s.name() == statusFilter}"` pre-selects the current filter. The option text uses i18n:
`th:text="#{${'status.' + s}}"` resolves to "Registered", "Paid", or "Canceled" (or Ukrainian equivalents). Same pattern
used for role filtering in admin panel.

---

## 26. LOCALE-AWARE DATE FORMATTING

**Q: How are dates formatted in templates?**
A: Using Thymeleaf's `#temporals` utility object: `${#temporals.format(date, 'dd MMM yyyy')}`. This is part of the
`thymeleaf-extras-java8time` module (included in Spring Boot starter). It works with Java 8+ temporal types —
`LocalDate`, `LocalDateTime`, `ZonedDateTime`. The `#temporals` object is analogous to `#dates` for legacy
`java.util.Date` but designed for the modern `java.time` API.

**Q: What does the pattern 'dd MMM yyyy' mean?**
A: Each letter is a pattern symbol from `java.time.format.DateTimeFormatter`: `dd` — day of month, zero-padded (01, 15,
31). `MMM` — abbreviated month name, locale-sensitive (Jan/Feb/Mar in English, січ./лют./бер. in Ukrainian). `yyyy` —
four-digit year. Together: `15 Jul 2026` in English, `15 лип. 2026` in Ukrainian. For booking timestamps we use
`'dd MMM yyyy HH:mm'` — adds hours (HH, 24-hour) and minutes (mm): `15 лип. 2026 10:30`.

**Q: How does #temporals know which locale to use?**
A: Thymeleaf resolves the current locale from Spring's `LocaleResolver`. In our app, `SessionLocaleResolver` stores the
selected locale in the HTTP session. When the user switches language via `?lang=uk` or `?lang=en`,
`LocaleChangeInterceptor` updates the session locale. On the next request, `#temporals.format()` picks up that locale
and formats month names accordingly — no explicit locale parameter needed in the template.

**Q: Why 'dd MMM yyyy' and not just outputting the raw LocalDate?**
A: Raw `LocalDate.toString()` outputs ISO format: `2026-07-15` — always English, not user-friendly. With
`'dd MMM yyyy'`: 1) Month names are localized (лип. vs Jul). 2) Day comes first (European convention, natural for
Ukrainian users). 3) Month as text is more readable than a number (Jul vs 07). 4) Consistent formatting across all
pages — catalogue cards, detail page, my-vouchers table, booking dates.

**Q: What's the difference between #temporals and #dates?**
A: `#dates` works with legacy `java.util.Date` and `java.util.Calendar`. `#temporals` works with `java.time` types (
`LocalDate`, `LocalDateTime`, `Instant`, etc.). Since our entities use `LocalDate` for arrival/eviction and
`LocalDateTime` for booking creation, `#temporals` is the correct choice. Using `#dates` on a `LocalDate` would throw a
conversion error. The `#temporals` utility also has helper methods like `#temporals.day(date)`,
`#temporals.month(date)`, `#temporals.year(date)` for extracting individual components.

**Q: Why is the default locale set to Ukrainian?**
A: The app is built for an EPAM Ukraine course project — Ukrainian users are the primary audience.
`SessionLocaleResolver` with `setDefaultLocale(new Locale("uk"))` means first-time visitors see the Ukrainian interface
immediately without needing to switch. Users who prefer English can switch via the language selector (`?lang=en`), and
their choice is stored in the session for subsequent requests.

**Q: How does locale affect date input fields in forms?**
A: It doesn't — `<input type="date">` always uses ISO format (`yyyy-MM-dd`) internally, regardless of locale. The
browser may display the date in the user's OS locale, but the submitted value is always ISO. This is an HTML5 standard.
Our `@DateTimeFormat` on DTO fields (or Spring's default `LocalDate` binding) expects ISO format, so form submission
works correctly across all locales. Only the display formatting in `th:text` is locale-aware.

---

## 27. UNAUTHENTICATED USER UX

**Q: What does an unauthenticated user see on the voucher detail page?**
A: Instead of the "Order" button, they see a "Login to Order" link (`btn-outline-success`) that redirects to `/login`.
This is better than hiding the action completely — the user knows ordering is possible, they just need to sign in. After
login, Spring Security redirects them back (default success URL), so the flow is seamless. The catalogue list page does
NOT show this button — it would clutter the card grid. Users browse the catalogue freely, click "Details" to learn more,
and see the login prompt there.

**Q: Why not just redirect unauthenticated users who click Order?**
A: Two reasons: 1) The Order button is a POST form (CSRF-protected). An unauthenticated POST would redirect to login,
but the POST data is lost — the user would land on the login page without context. 2) UX clarity — showing "Login to
Order" is explicit. The user understands they need an account before they click, not after a confusing redirect.

**Q: How does the template decide which button to show?**
A: Thymeleaf's Spring Security integration:
`th:if="${#authorization.expression('isAuthenticated()') and voucher.availableQuantity > 0}"` shows the Order form for
logged-in users with available spots.
`th:if="${!#authorization.expression('isAuthenticated()') and voucher.availableQuantity > 0}"` shows the Login link for
guests. If sold out, neither appears — both conditions check `availableQuantity > 0`.

---

## 28. FILTERING ARCHITECTURE OVERVIEW

**Q: How many pages in the app have filtering and what filters does each support?**
A: Four pages with filtering, all using the same JPA Specification pattern: 1) **Catalogue** (public) — tour type,
transfer type, hotel type, min/max price, title search, sort (price/discount/hot). 2) **Manager Vouchers** — title
search, date filter (all/active/expired), tour type. 3) **Manager Orders** — voucher title search, username search,
booking status. 4) **Admin Users** — username search, role filter, status filter (active/blocked). Each uses its own
Specification class (VoucherSpecification, BookingSpecification, UserSpecification).

**Q: What is the common pattern across all filtering implementations?**
A: Five consistent steps in every implementation: 1) Controller accepts optional `@RequestParam` filters. 2) Service
builds `Specification.where(null)` as base, conditionally chains `.and(...)` for each non-empty filter. 3) Repository
extends `JpaSpecificationExecutor<T>` providing `findAll(Specification, Pageable)`. 4) Template renders filter form with
current values pre-filled (`th:value`, `th:selected`). 5) Pagination links include all filter params to preserve state
across pages. This consistency makes the code predictable — once you understand one page's filtering, you understand all
of them.

**Q: How do enum dropdowns work consistently across all filter forms?**
A: Same three-step pattern everywhere: 1) Controller adds enum values to model —
`model.addAttribute("tourTypes", TourType.values())`. 2) Template iterates with `th:each="t : ${tourTypes}"`, sets
`th:value="${t}"` for the form value and `th:text="#{${'tourType.' + t}}"` for the i18n display label. 3)
`th:selected="${t.name() == tourTypeFilter}"` pre-selects the current filter. The i18n key pattern `prefix.ENUM_VALUE` (
e.g., `tourType.ADVENTURE`, `role.ADMIN`, `status.PAID`) is consistent across all enums.

**Q: Why pass individual filter parameters instead of a DTO for manager/admin pages?**
A: The catalogue uses `VoucherFilterDTO` because it has 7 filter fields — a DTO keeps the controller signature clean.
Manager vouchers (3 filters), manager orders (3 filters), and admin users (3 filters) each have few enough params that
`@RequestParam` is simpler — no DTO class needed, Spring binds them directly. If any of these grew beyond 4-5 filters,
extracting a DTO would be the right refactoring step.

---

## 29. DUPLICATE EMAIL VALIDATION

**Q: How do you prevent duplicate emails during registration?**
A: UserServiceImpl.createUser() checks `userRepository.existsByEmail(email)` before saving. If a user with that email already exists, it throws `DuplicateUsernameException` with message key `error.user.email.duplicate`. AuthController catches `DuplicateUsernameException` (same catch block handles both username and email duplicates) and displays the localized error on the registration form: "Email 'x@y.com' is already in use".

**Q: How do you prevent duplicate emails during profile update?**
A: UserServiceImpl.updateProfile() has a two-condition check: `if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail()))`. The first condition (`!equals`) skips the check if the user submits their own current email — only new emails are checked for duplicates. The second condition queries the database. If both are true, same `DuplicateUsernameException` is thrown. UserController catches it and shows the error via flash attribute redirect.

**Q: Why reuse DuplicateUsernameException for email duplicates instead of creating a new exception?**
A: The exception class is already generic — it takes a message key and args, implements `LocalizedException`, and is caught by both AuthController and GlobalExceptionHandler. Creating `DuplicateEmailException` would be identical in structure — same fields, same interface, same catch behavior. The message key (`error.user.email.duplicate` vs `error.user.duplicate`) determines what the user sees. The exception class is the transport mechanism, the message key is the content.

**Q: Why is the email check in the service layer and not a @Unique annotation?**
A: Three reasons: 1) JPA doesn't have a standard `@Unique` validation annotation — `@Column(unique = true)` is a schema constraint, not Bean Validation. 2) The database UNIQUE constraint is still there as a safety net (defense in depth), but relying on it alone would produce a `DataIntegrityViolationException` — a generic 500 error with a cryptic SQL message. 3) The service check provides a clean, localized error message that the controller can display to the user.

**Q: What happens if two users submit the same email at the exact same moment?**
A: The service-level check has a tiny race condition window. Both threads pass the `existsByEmail` check, both try to INSERT. The database UNIQUE constraint catches the second one — `DataIntegrityViolationException` is thrown. GlobalExceptionHandler catches it and shows a generic error page. For a course project this is acceptable. In production, you'd add a retry mechanism or use `INSERT ... ON CONFLICT` with a custom repository query.

---

## 30. REGISTRATION vs PROFILE UPDATE VALIDATION

**Q: Why are RegisterDTO and UserUpdateDTO separate classes?**
A: Different validation rules for different use cases. Registration requires all fields (username, password, email, firstName, lastName) — the user is creating a new account from scratch. Profile update makes everything optional — the user might only want to change their phone number without re-entering their email and password. Two DTOs with different annotations are cleaner than one DTO with conditional validation.

**Q: How does email validation differ between RegisterDTO and UserUpdateDTO?**
A: RegisterDTO: `@NotBlank` + `@Email(regexp = ".+@.+\\..+")` — email is required and must be valid. UserUpdateDTO: `@Email(regexp = "^$|.+@.+\\..+")` — email is optional (empty string passes) but if provided must be valid. The `^$|` prefix in the regex means "empty string OR valid email". This way, submitting the profile form without touching the email field doesn't trigger a validation error.

**Q: How does the service handle blank fields in profile update?**
A: Each field is checked individually: `if (request.getFirstName() != null && !request.getFirstName().isBlank()) user.setFirstName(...)`. Blank or null fields are silently skipped — only non-empty fields update the entity. This means submitting a form with only the phone number filled in updates only the phone number. Password follows the same pattern — blank password means "don't change it".

**Q: Why does @StrongPassword on UserUpdateDTO not block empty passwords?**
A: The custom StrongPasswordValidator has a null/blank check at the beginning: if the password is null or blank, it returns `true` (valid). This is intentional — on profile update, blank password means "keep current password". On RegisterDTO, `@NotBlank` runs first and catches empty passwords before `@StrongPassword` even executes.

**Q: How does the @Pattern on phoneNumber handle optional input?**
A: The regex is `^$|^\\+?\\d{10,15}$` — same "empty OR valid" pattern as the email. `^$` matches empty string (user didn't enter a phone), `^\\+?\\d{10,15}$` matches a valid phone number (optional + prefix, 10-15 digits). This works on both RegisterDTO and UserUpdateDTO since phone is optional in both cases.

---

## 31. DASHBOARD NAVIGATION

**Q: How do the dashboard stat cards work as navigation links?**
A: Each stat card is wrapped in an `<a>` tag that links to the relevant management page with a pre-applied filter. For example, clicking "Active Users" navigates to `/admin/users?statusFilter=active`, clicking "Registered Orders" navigates to `/manager/orders?statusFilter=REGISTERED`. The `text-decoration-none` CSS class removes the link underline so the cards look like cards, not hyperlinks.

**Q: What are all the dashboard card links?**
A: Six cards with six destinations: 1) Total Users → `/admin/users` (no filter — all users). 2) Active Users → `/admin/users?statusFilter=active` (pre-filtered to active). 3) Available Vouchers → `/manager/vouchers?dateFilter=active` (pre-filtered to upcoming tours). 4) Registered Orders → `/manager/orders?statusFilter=REGISTERED`. 5) Paid Orders → `/manager/orders?statusFilter=PAID`. 6) Canceled Orders → `/manager/orders?statusFilter=CANCELED`. This turns the dashboard from a static display into an actionable navigation hub — the admin sees a number and can immediately click to see the details behind it.

**Q: Why can the admin access /manager/* routes?**
A: SecurityConfig grants `/manager/**` access to both MANAGER and ADMIN roles: `requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")`. The admin has a superset of manager permissions — they can view and manage vouchers and orders in addition to managing users. This is standard RBAC hierarchy.

---

## 32. PASSWORD CONFIRMATION

**Q: How does the password confirmation work?**
A: Custom class-level `@PasswordMatch` annotation with `PasswordMatchValidator`. The validator implements `ConstraintValidator<PasswordMatch, PasswordConfirmable>` — it compares `getPassword()` and `getConfirmPassword()`. If they don't match, it adds a validation error to the `confirmPassword` field using `context.buildConstraintViolationWithTemplate().addPropertyNode("confirmPassword")`. Same pattern as `@DateRange` for voucher dates.

**Q: What is PasswordConfirmable?**
A: An interface with two methods: `getPassword()` and `getConfirmPassword()`. Both `RegisterDTO` and `UserUpdateDTO` implement it. This allows one validator to work with both DTOs — the validator doesn't know or care which DTO it's validating, it just calls the interface methods. This follows the Interface Segregation Principle — the validator depends on a minimal contract, not a concrete class.

**Q: How does password confirmation work on profile update when password is optional?**
A: The validator checks `if (password == null && confirmPassword == null) return true`. When the user submits the profile form without touching the password fields, both are null — validation passes, and the service skips the password update (`if (request.getPassword() != null && !request.getPassword().isBlank())`). If the user fills in only one of the two fields, the validator catches the mismatch.

**Q: Why a class-level annotation instead of a field-level annotation?**
A: Field-level validators can only see one field. Password confirmation needs to compare two fields (`password` and `confirmPassword`) which requires access to the whole object. Class-level annotations (`@Target(ElementType.TYPE)`) receive the entire DTO instance, enabling cross-field validation. Same approach used for `@DateRange` (compares arrivalDate and evictionDate).

**Q: How does the role dropdown auto-submit work in the user management table?**
A: The `<select>` element has `onchange="this.form.submit()"` — a minimal JavaScript one-liner. When the admin selects a new role from the dropdown, the form submits immediately without needing a separate "Save" button. This is a common UX pattern for single-field forms — reduces clicks and keeps the table compact. The `this.form` reference navigates from the select element to its parent form.

---

## 33. ADMIN USER DETAIL & PASSWORD RESET

**Q: How does the admin view user details?**
A: `GET /admin/users/{id}` renders `admin/user-detail.html` — a read-only page showing all user fields: username, first/last name, email, phone, balance, role (colored badge), status (active/blocked badge). No edit fields — admin views information only. Actions are on the user list page (block/unblock, role change) and the detail page (password reset).

**Q: How does password reset work?**
A: `POST /admin/users/{id}/reset-password` calls `UserManagementServiceImpl.resetPassword()`. It loads the user, validates the admin isn't resetting their own password (`validateNotSelf()`), encodes the default password `"12345678"` with `BCryptPasswordEncoder`, and saves. The admin sees a success message "Password has been reset to default". The user can then login with `12345678` and change it via their profile.

**Q: Why a hardcoded default password instead of generating a random one?**
A: Simplicity for a course project. The admin needs to communicate the new password to the user — a fixed default is easy to remember and share. In production, you'd generate a random password and send it via email, or use a token-based "forgot password" flow with an expiry link.

**Q: Why can't the admin reset their own password?**
A: Same `validateNotSelf()` check used for block/unblock and role change. Resetting your own password to a weak default (`12345678`) would be a security downgrade. The admin should change their own password through the profile page with the strong password validator.

**Q: Why is the reset button hidden for the current admin on the detail page?**
A: `th:if="${user.username != currentUsername}"` — same pattern used in the user list table. The admin's own detail page shows all info but no reset button, preventing accidental self-reset. The controller also has the server-side `validateNotSelf()` check as defense in depth.
