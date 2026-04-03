# Travel Agency — Presentation Preparation Guide

## Root Configuration Files

### pom.xml
**Why:** Maven build config. Spring Boot 3.4.4 parent, Java 17.
**Key dependencies:** spring-boot-starter-data-jpa, security, web, thymeleaf, validation, aop, h2, postgresql, lombok, thymeleaf-extras-springsecurity6.

**Q: Why thymeleaf-extras-springsecurity6?**
A: Enables `sec:authorize` in templates — show/hide elements based on user role (e.g., admin menu only for ADMIN).

**Q: Why no MapStruct dependency?**
A: We use manual mappers. Only 3 mappers with custom logic — MapStruct would need more configuration than the manual code.

**Q: Why spring-boot-starter-aop?**
A: For AOP-based logging with `@Aspect`. Cross-cutting concern — we log method entry/exit and business events without polluting service code.

---

### docker-compose.yml
**Why:** Runs PostgreSQL 16 in Docker for production profile.
**Key details:** Port 5433 (to avoid conflict with local PostgreSQL), env vars from .env file, UTC timezone (PostgreSQL 16 renamed Europe/Kiev to Europe/Kyiv — causes errors with JVM).

**Q: Why port 5433 and not 5432?**
A: Developer has local PostgreSQL on 5432. Docker maps 5433 (host) -> 5432 (container).

**Q: Why TZ=UTC and PGTZ=UTC?**
A: PostgreSQL 16 doesn't recognize "Europe/Kiev" timezone. Setting UTC avoids timezone mismatch between JVM and DB.

---

### .env
**Why:** Stores database credentials. Gitignored — not committed to repo. Docker Compose and Spring Boot both read it.

**Q: How does Spring Boot read .env?**
A: Via `spring.config.import: optional:file:.env[.properties]` in application-prod.yml. The `optional:` prefix means app won't fail if .env doesn't exist.

**Q: Why not hardcode credentials?**
A: Security — credentials should never be in version control. Each environment has its own .env.

---

### .gitignore
**Why:** Excludes IDE files, build output, .env (secrets), logs directory.

---

## Application Entry Point

### Application.java
**Why:** Spring Boot main class. Sets JVM timezone to UTC before context starts.

**Q: Why TimeZone.setDefault(UTC) in main()?**
A: PostgreSQL 16 rejects "Europe/Kiev" timezone. Setting UTC before Spring context starts ensures JDBC driver sends "UTC" during connection setup.

**Q: Why not @PostConstruct?**
A: @PostConstruct runs after bean creation — too late. DataSource is created during context initialization, before @PostConstruct executes.

---

## Config Package

### ApplicationConfig.java
**Why:** Configures Spring Security beans: UserDetailsService (loads user from DB), AuthenticationProvider (DaoAuthenticationProvider with BCrypt), PasswordEncoder.

**Q: Why DaoAuthenticationProvider and not default?**
A: We need DB-backed authentication. DaoAuthenticationProvider uses our UserDetailsService to load users and BCryptPasswordEncoder to verify passwords.

**Q: Why is UserDetailsService a lambda?**
A: It's a functional interface with one method. The lambda calls `userRepository.findUserByUsername()` — concise and readable.

---

### SecurityConfig.java
**Why:** Configures HTTP security: URL-based authorization rules, form login, logout, CSRF, blocked user filter.

**Q: Why CSRF is enabled?**
A: Thymeleaf is server-side rendering with forms. CSRF protection prevents cross-site request forgery. Thymeleaf's `th:action` automatically includes CSRF tokens.

**Q: Why addFilterAfter(blockedUserFilter)?**
A: Checks if authenticated user is blocked on every request. Placed after UsernamePasswordAuthenticationFilter so authentication happens first.

**Q: What's the URL security hierarchy?**
A: Public: /, /catalogue, /login, /register, static resources. Authenticated: /profile, /my-vouchers. MANAGER+ADMIN: /manager/**. ADMIN only: /admin/**.

---

### BlockedUserFilter.java
**Why:** OncePerRequestFilter that checks if the logged-in user is still active. If blocked by admin, invalidates session and redirects to /login?blocked=true.

**Q: Doesn't this hit the DB on every request?**
A: Yes, tradeoff between security and performance. In production, could cache user status in Redis with short TTL. For this project, correctness is prioritized.

**Q: Why shouldNotFilter() skips certain paths?**
A: No need to check blocked status for public pages (login, register, static resources). Reduces unnecessary DB queries.

---

### WebConfig.java
**Why:** Configures i18n: MessageSource (loads messages_en/uk.properties), LocaleResolver (session-based), LocaleChangeInterceptor (?lang=en/uk), and connects MessageSource to bean validation.

**Q: How does language switching work?**
A: User clicks ?lang=uk -> LocaleChangeInterceptor catches "lang" param -> stores locale in session -> all subsequent requests use that locale.

**Q: Why LocalValidatorFactoryBean with MessageSource?**
A: Connects Jakarta Bean Validation to Spring's MessageSource. Validation error messages like `{validation.user.username.required}` are resolved from properties files.

---

## Model Package

### User.java
**Why:** JPA entity implementing UserDetails for Spring Security integration. Fields: id, username, password, role, firstName, lastName, email, phoneNumber, balance, active, bookings.

**Q: Why implements UserDetails?**
A: Spring Security needs UserDetails for authentication. Instead of creating a separate adapter class, the entity itself implements the interface — simpler.

**Q: Why @Builder.Default on balance and active?**
A: Lombok's @Builder doesn't use field initializers. Without @Builder.Default, `User.builder().build()` would set balance=null and active=false instead of BigDecimal.ZERO and true.

**Q: Why isAccountNonLocked() returns active?**
A: When admin blocks a user (active=false), Spring Security treats it as locked account. Login will fail with "account is locked" message.

---

### Voucher.java
**Why:** Tour/voucher entity. Fields: title, description, price, tourType, transferType, hotelType, status (AVAILABLE/DISABLED), dates, isHot, discount, quantity, bookings.

**Q: Why separate VoucherStatus from BookingStatus?**
A: Different concerns. Voucher status is about availability (AVAILABLE/DISABLED). Booking status is about order lifecycle (REGISTERED/PAID/CANCELED). Single Responsibility Principle.

**Q: Why quantity field?**
A: Multiple users can book the same tour. Quantity defines total spots. Available spots = quantity - active bookings (calculated at service layer, not stored).

---

### Booking.java
**Why:** Join entity between User and Voucher. Stores order state: status, bookedPrice (price snapshot at booking time), createdAt.

**Q: Why bookedPrice instead of reading voucher.price?**
A: Price snapshot — if manager changes price/discount after booking, refund should use the original price the user paid. Standard e-commerce practice.

**Q: Why ManyToOne with LAZY fetch?**
A: Performance — don't load User and Voucher for every Booking query. Load on demand. For paginated lists, we use @EntityGraph to fetch eagerly when needed.

---

### Enums (Role, TourType, TransferType, HotelType, VoucherStatus, BookingStatus)
**Why:** Type-safe constants stored as strings in DB (EnumType.STRING). Role.getAuthorities() returns Spring Security authorities with "ROLE_" prefix.

**Q: Why EnumType.STRING and not ORDINAL?**
A: ORDINAL stores position number — if enum order changes, existing data breaks. STRING stores the name — safe for refactoring.

---

## DTO Package

### Request DTOs (RegisterDTO, UserUpdateDTO, VoucherCreateDTO, TopUpDTO, ChangeRoleDTO, ChangeStatusDTO, VoucherFilterDTO)
**Why:** Separate input DTOs with validation annotations. Never expose entity internals to the view layer.

**Q: Why separate RegisterDTO and UserUpdateDTO?**
A: Different validation rules. Registration requires username, password, name, email (all @NotBlank). Update allows partial — all fields optional.

**Q: What's @StrongPassword?**
A: Custom validation annotation. Checks: min 8 chars, uppercase, lowercase, digit, special character. Implemented via StrongPasswordValidator.

**Q: What's @DateRange?**
A: Class-level custom validator on VoucherCreateDTO. Ensures evictionDate > arrivalDate. Applied at class level because it needs two fields.

**Q: Why VoucherFilterDTO has no validation?**
A: Filter params are optional and don't modify data. Invalid filter values (e.g., non-existent tourType) throw IllegalArgumentException caught by global handler.

---

### Response DTOs (UserDTO, VoucherDTO, BookingDTO, StatsDTO)
**Why:** Control what data is sent to the view. UserDTO has NO password field — security.

**Q: Why VoucherDTO has availableQuantity?**
A: Computed field: quantity - active bookings. Calculated in VoucherServiceImpl.toEnrichedDTO(), not stored in DB. Shows remaining spots in catalogue.

**Q: Why BookingDTO denormalizes voucher fields (voucherTitle, tourType, dates)?**
A: Avoids accessing lazy-loaded Voucher in templates. All needed data is in the DTO — no N+1 queries in view layer.

---

## Repository Package

### UserRepository.java
**Why:** JPA repository with custom queries: findUserByUsername, existsByUsername, findAllByRole, countAllUsers, countActiveUsers.

**Q: Why @Query for countAllUsers instead of count()?**
A: JpaRepository.count() works too, but explicit @Query shows custom query implementation for the course requirements.

---

### VoucherRepository.java
**Why:** Extends JpaSpecificationExecutor for dynamic filtering. Only one custom method: countByStatus.

**Q: Why JpaSpecificationExecutor?**
A: Enables Criteria API queries built at runtime. Catalogue has 6 filter criteria — instead of 64 repository method combinations, one `findAll(Specification, Pageable)` handles all.

---

### BookingRepository.java
**Why:** Booking queries with @EntityGraph to eagerly fetch User and Voucher. Custom queries: countActiveBookingsByVoucherId, sumPaidRevenue.

**Q: What does @EntityGraph do?**
A: Tells JPA to JOIN FETCH related entities in a single query. Without it, accessing booking.getUser() triggers a separate query per booking (N+1 problem).

**Q: Why override findAll and findById?**
A: To add @EntityGraph to inherited methods. By default, JpaRepository methods don't join fetch — we override to add eager loading.

---

### VoucherSpecification.java
**Why:** Static factory methods that create Specification<Voucher> predicates. Combinable with `.and()` for multi-criteria filtering.

**Q: How is this SQL injection safe?**
A: JPA Criteria API uses parameterized queries internally. The `cb.like()`, `cb.equal()` methods generate prepared statements — user input is never concatenated into SQL.

**Q: Why escapeLikeWildcards()?**
A: Users could type % or _ in search — these are LIKE wildcards. We escape them so they're treated as literal characters.

---

## Service Package

### AuthenticationService / AuthenticationServiceImpl
**Why:** Thin orchestration layer. Delegates to UserService.createUser(). Login is handled by Spring Security — no custom code needed.

**Q: Why not register directly in controller?**
A: Separation of concerns. If we add OAuth2 login later, it also calls UserService — single entry point for user creation.

---

### UserService / UserServiceImpl
**Why:** User CRUD: create, get, update profile, top up balance. Owns all UserRepository access.

**Q: Why DuplicateUsernameException extends LocalizedException?**
A: Implements LocalizedException interface — carries message key + args. GlobalExceptionHandler resolves the i18n message via MessageSource.

**Q: Why separate topUpBalance method?**
A: Different business logic than updateProfile. TopUp adds to balance (additive), update replaces fields. Different validation — TopUp needs @DecimalMin(0.01).

---

### UserManagementService / UserManagementServiceImpl
**Why:** Admin-only operations: block, unblock, change role. Separated from UserService — different responsibility (Single Responsibility Principle).

**Q: Why @PreAuthorize on class level?**
A: All methods in this service require ADMIN role. Class-level annotation avoids repeating on every method.

**Q: Why validateNotSelf()?**
A: Admin can't block/unblock/change role of their own account. Prevents accidentally locking yourself out.

---

### VoucherService / VoucherServiceImpl
**Why:** Voucher CRUD + filtering. Uses VoucherSpecification for dynamic queries. Enriches DTOs with availableQuantity.

**Q: Why toEnrichedDTO()?**
A: VoucherMapper doesn't know about BookingRepository. The service calculates availableQuantity (quantity - active bookings) and adds it to the DTO.

**Q: Why validate quantity in update()?**
A: Can't set quantity below active bookings. If 5 users booked and manager tries to set quantity=3, throws InvalidOrderStatusException.

---

### OrderService / OrderServiceImpl
**Why:** Cross-domain transaction service. Creates Bookings, manages balance, handles cancellations and status changes.

**Q: Why does OrderService access repositories directly?**
A: Ordering touches User (balance deduction) and Booking (creation) atomically. Going through UserService + VoucherService would split the @Transactional boundary.

**Q: How does the discount calculation work?**
A: `discountedPrice = price * (100 - discount) / 100`. Uses BigDecimal for precision. Stored in Booking.bookedPrice — refunds use this stored price, not current voucher price.

**Q: What happens when manager cancels a booking?**
A: Refunds bookedPrice to user balance, sets booking status to CANCELED. The voucher spot becomes available for other users (activeBookings count decreases).

**Q: What about race conditions?**
A: Two users could order the last spot simultaneously. Both pass the quantity check. Fix: optimistic locking (@Version) or SELECT FOR UPDATE. Not implemented — known limitation.

---

### StatsService / StatsServiceImpl
**Why:** Dashboard aggregation. Counts users, bookings by status, revenue. Uses BookingRepository for order stats, VoucherRepository for available count.

**Q: Why separate service for stats?**
A: Read-only queries with different access pattern (aggregation vs CRUD). Separate @Transactional(readOnly=true) optimization.

---

## Mapper Package

### UserMapper / UserMapperImpl
**Why:** Manual mapper. toUserDTO() — never includes password. toUser() — reverse mapping (used for import/integration).

### VoucherMapper / VoucherMapperImpl
**Why:** Three methods: toVoucher(VoucherDTO), toVoucher(VoucherCreateDTO), updateVoucherFromDTO(), toVoucherDTO(). Handles enum String<->Enum conversion.

### BookingMapper / BookingMapperImpl
**Why:** Maps Booking -> BookingDTO with denormalized fields from User and Voucher (username, voucherTitle, tourType, dates).

**Q: Why manual mappers instead of MapStruct?**
A: Only 3 mappers with custom logic (null checks, enum conversion, computed fields). MapStruct would need @Mapping annotations for every custom field — more config than manual code.

---

## Exception Package

### Exception Hierarchy
- `ResourceNotFoundException` (abstract parent) -> `UserNotFoundException`, `VoucherNotFoundException`, `BookingNotFoundException`
- `LocalizedException` (interface) -> `DuplicateUsernameException`, `InsufficientBalanceException`, `InvalidOrderStatusException`
- `UserBlockedException`

### GlobalExceptionHandler
**Why:** @ControllerAdvice that catches all exceptions and returns Thymeleaf error views (400, 403, 404, 500). Uses MessageSource for i18n error messages.

**Q: What's LocalizedException interface?**
A: Carries messageKey + args. GlobalExceptionHandler calls `messageSource.getMessage(key, args, locale)` to resolve localized message. Exceptions thrown in service layer with message keys, resolved in handler with current locale.

**Q: Why not put MessageSource in services?**
A: Services shouldn't depend on presentation concerns. Message resolution is a view-layer responsibility. Services throw exceptions with keys, handler resolves them.

---

## Validation Package

### @StrongPassword + StrongPasswordValidator
**Why:** Custom constraint annotation. Checks min 8 chars + uppercase + lowercase + digit + special char. Uses precompiled Pattern constants for performance.

### @DateRange + DateRangeValidator
**Why:** Class-level validator on VoucherCreateDTO. Ensures evictionDate > arrivalDate. Adds error to "evictionDate" field specifically.

**Q: Why class-level and not field-level?**
A: It compares two fields (arrivalDate and evictionDate). Field-level validators only see one field. Class-level gets the whole object.

---

## Aspect Package

### LoggingAspect
**Why:** AOP cross-cutting logging. Pointcuts on service.impl package.
- @Before: logs method entry with args (DEBUG level)
- @AfterReturning: logs method exit (DEBUG level)
- @AfterThrowing: logs exceptions (ERROR level)
- @Around on specific methods: logs business events (INFO/WARN level)

**Q: Why @Around for specific methods?**
A: Business events need before+after logging with specific messages. @Around wraps the method call — can log before, execute, log after.

**Q: Why not log passwords?**
A: RegisterDTO logging extracts only username: `((RegisterDTO) args[0]).getUsername()`. Never logs the full DTO which contains password.

### SecurityEventListener
**Why:** Listens to Spring Security events: AuthenticationSuccessEvent and AbstractAuthenticationFailureEvent. Logs login success/failure with username.

---

## Controller Package

### AuthController
**Why:** Login page (GET /login), register page + form (GET/POST /register). Catches DuplicateUsernameException and shows localized error.

### CatalogueController
**Why:** Public catalogue (GET /catalogue), voucher detail (GET /catalogue/{id}), order action (POST /catalogue/{id}/order). Uses VoucherFilterDTO for multi-criteria filtering.

### UserController
**Why:** Profile (GET /profile), top-up (POST /profile/topup), update (POST /profile/update), my bookings (GET /my-vouchers), cancel (POST /my-vouchers/{id}/cancel).

### ManagerController
**Why:** Voucher CRUD (list, create, edit, delete), order management (list orders, change status). Search by title.

### AdminController
**Why:** User management (list, block, unblock, role change), dashboard stats. Passes currentUsername to template for self-protection UI.

**Q: How is CSRF handled in forms?**
A: Thymeleaf's `th:action="@{...}"` automatically includes CSRF token as hidden field. No manual token management needed.

**Q: Why @AuthenticationPrincipal?**
A: Injects the current logged-in user directly into controller method. Avoids SecurityContextHolder.getContext().getAuthentication() boilerplate.

---

## Resources

### application.yml
**Why:** Common config for all profiles. Server port, error config (no stack traces), JPA settings, date format, Thymeleaf cache disabled. Active profile set here.

### application-test.yml
**Why:** H2 in-memory DB, ddl-auto=none (schema.sql manages tables), loads data-h2.sql with [TEST] prefixed vouchers.

### application-prod.yml
**Why:** PostgreSQL connection via env vars from .env file. ddl-auto=none, loads schema.sql + data.sql with production data.

**Q: Why ddl-auto=none?**
A: Schema managed by SQL scripts — full control over table structure, constraints, indexes. Hibernate auto-generation doesn't create CHECK constraints or custom indexes.

**Q: Why spring.sql.init.mode=always?**
A: Executes schema.sql and data.sql on every startup. Schema uses CREATE TABLE IF NOT EXISTS, data uses ON CONFLICT DO NOTHING — idempotent.

---

### schema.sql
**Why:** Database schema with constraints. Three tables: user, voucher, booking. CHECK constraints on enums, prices, balance. Foreign keys. Indexes on filtered columns.

**Q: Why quoted "user" table name?**
A: "user" is a SQL reserved word. Quotes prevent syntax errors. Hibernate's auto_quote_keyword=true handles this in JPA queries.

---

### messages_en.properties / messages_uk.properties
**Why:** i18n bundles. ~250 keys each. Categories: validation errors, navigation, common UI, auth, catalogue, profile, manager, admin, success/error messages, enum translations.

---

### logback-spring.xml
**Why:** Profile-specific logging config. Test: DEBUG to console. Prod: INFO to console + file (rolling daily, 30 days). Default: INFO to console.

---

## Test Files

### Service Tests (Mockito)
**Why:** Unit tests with mocked repositories. @ExtendWith(MockitoExtension.class), @Nested for grouping, @DisplayName for readability. Test happy paths, error cases, edge cases.

### Controller Tests (@WebMvcTest)
**Why:** Tests web layer only. MockMvc simulates HTTP requests. @MockitoBean for services. @WithMockUser for authentication. Tests: status codes, view names, model attributes, security (403 for wrong role).

### ApplicationTests
**Why:** Integration test. @SpringBootTest loads full context with H2. Verifies schema + data + Spring beans all work together.

**Q: Why 92 tests total?**
A: 63 service unit tests (all 6 services covered with edge cases) + 29 controller tests (all 5 controllers) = 92.
