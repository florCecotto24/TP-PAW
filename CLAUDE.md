# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
mvn clean install

# Run the web application (available at http://localhost:8080/)
mvn jetty:run -pl webapp

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl persistence

# Run a single test class
mvn test -pl persistence -Dtest=ListingJdbcDaoTest
```

The app is served at `http://localhost:8080/` (context path is `/webapp` per `application.properties`, so routes like `/webapp/home`).

## Database

Database credentials are in `webapp/src/main/resources/application.properties`. The schema is auto-applied on startup from `persistence/src/main/resources/schema.sql` via `DataSourceInitializer` in `WebConfig.java`. The schema uses `CREATE TABLE IF NOT EXISTS`, so it is safe to re-run.

Tests use an **in-memory HSQLDB** database configured in each module's `TestConfiguration.java` — no PostgreSQL required for testing.

### Flyway migrations

Schema bootstrap runs first (`schema.sql` via `DataSourceInitializer`), then Flyway applies migrations from `classpath:db/migration/` (`webapp/src/main/resources/db/migration/`). Flyway config in `WebConfig.java`: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

New migrations must follow the naming convention `V<number>__<description>.sql` (e.g. `V4__add_reviews.sql`). Existing migrations: `V2__users_extend_profile_and_auth.sql`, `V3__email_verification_codes.sql`.

## Architecture

This is a **car rental platform** built as a multi-module Maven project with strict layer separation.

### Module dependency chain

```
webapp → services → persistence → models
              ↑            ↑
   service-contracts  persistence-contracts
```

- **models** — Domain POJOs: `Car`, `User`, `Listing`, `Reservation`, `Image`, `CarPicture`, plus DTOs `ListingCard` and `ListingDetail`.
- **persistence-contracts** — DAO interfaces (e.g., `ListingDao`, `CarDao`).
- **persistence** — JDBC implementations using `JdbcTemplate` with inline SQL. DAO tests live here and run against HSQLDB.
- **service-contracts** — Service interfaces (e.g., `ListingService`, `ReservationService`).
- **services** — Service implementations containing business logic. Service tests mock DAOs with Mockito.
- **webapp** — Spring MVC controllers, JSP views, and all Spring configuration.

### Key conventions

- **Dependency injection**: Constructor injection with `@Autowired`.
- **Persistence**: Plain SQL via `JdbcTemplate`. No ORM. `RowMapper` classes defined inline or as private static fields.
- **Configuration**: Java-based (`WebConfig.java`, `SpringMailConfig.java`) bootstrapped from `web.xml`. Properties loaded from `application.properties`; profile-specific overrides load from `application-{profile}.properties` if present.
- **Dependency versions**: All versions declared in root `pom.xml` `<dependencyManagement>`. Child modules reference dependencies without versions.
- **Views**: JSPs in `webapp/src/main/webapp/WEB-INF/views/`. Reusable tag files in `WEB-INF/tags/`. Static assets (CSS, JS, images) served from `webapp/src/main/webapp/css/`, `/js/`, `/assets/`.
- **Component scan**: `WebConfig` scans `ar.edu.itba.paw.webapp.controller`, `ar.edu.itba.paw.webapp.util`, `ar.edu.itba.paw.webapp.security`, `ar.edu.itba.paw.webapp.validation`, `ar.edu.itba.paw.services`, and `ar.edu.itba.paw.persistence`.

### Domain overview

A `User` owns `Car`s. A `Car` can have a `Listing` (rental offer) with a price and availability periods (`listing_availability`). Other users create `Reservation`s against a listing. `Image`s are stored as byte arrays in the DB and associated to cars via `CarPicture`.

Key enums (defined inside model classes): `Car.Type`, `Car.Powertrain`, `Car.Transmission`, `Listing.Status` (active/paused/finished), `Reservation.Status` (accepted/started/cancelled/finished).

## Security

Configured in `WebAuthConfig` with Spring Security 5.7.14. Uses `@EnableWebSecurity`, `SecurityFilterChain`, custom `RydenAuthenticationProvider` and `RydenUserDetailsService`. Includes remember-me support, session-based auth, and CSRF protection. Do not add or replace auth config outside `WebAuthConfig`.

## Internationalization (i18n)

- **UI/errors**: `ReloadableResourceBundleMessageSource` with basenames `classpath:messages` and `classpath:exception-messages`. Default is English; Spanish uses `messages_es.properties` / `exception-messages_es.properties`.
- **Locale resolution**: `AcceptHeaderLocaleResolver` in `WebConfig` — reads `Accept-Language` header; supported locales English and Spanish (`es`); defaults to English.
- **`LocaleMessages`** (`webapp.util`): resolves message keys via `MessageSource` + `LocaleContextHolder`.
- **Mail**: Separate bundle `mail/MailMessages.properties` (English) and `mail/MailMessages_es.properties`. Because `@Async` mail methods run on a different thread, they do NOT inherit `LocaleContextHolder` — the request locale must be captured on the synchronous thread and passed in (e.g. via `ReservationConfirmationPayload#getMessageLocale`).
- **Exception message keys**: defined in `exception-messages.properties` and keyed via `ar.edu.itba.paw.exception.MessageKeys`.

## Testing

- **Unit tests**: JUnit 5 + Mockito in `services` and `models`.
- **Persistence tests**: HSQLDB (`TestPersistenceConfig`), integration-style DAO tests under `persistence/src/test`.
- **DAO integration tests** (`*JdbcDaoTest`, `DaoIntegrationTestSupport`): After a **write** (`create*`, `update*`, `delete*`), assert with **`JdbcTemplate`** or SQL fixtures in arrange — **never** confirm persistence via a **second method on the same DAO** (e.g. `createCar` + `getCarById`). Prefer `insert*` / SQL for setup when testing updates or deletes on that DAO. For ordered **read** results backed by SQL fixtures, optionally assert ordering against a matching `ORDER BY` query via `JdbcTemplate`.
- **Strict Mockito**: Unused stubbings fail with `UnnecessaryStubbingException` — remove any `when(...)` that isn't exercised by the test.
- **No interaction verification**: Do not use `Mockito.verify`, `verifyNoInteractions`, `verifyNoMoreInteractions`, `never()`, `times()`, `InOrder`, or captors to assert collaborator calls — tests assert outcomes (returns, models, state), not implementation wiring.
- **Do not emulate `verify`**: Avoid counters, collector lists, or stubs used only to assert call count/order. Helpers like `doAnswer` are acceptable only when asserting contract-level outputs/effects (for example, generated message content), not collaborator wiring.
- **Fixed literals in matchers**: Do not use `Mockito.anyLong()` (or `any*()`) as the value inside `when(...)` — use fixed literals so tests are deterministic and intention is clear.

## Logging

Use SLF4J (never Log4j or `java.util.logging`). Declare the logger as:

```java
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
```

Always use parameterized logging to avoid string construction overhead:
```java
LOGGER.debug("Creating user with email {}", email); // correct
LOGGER.debug("Creating user with email " + email);  // wrong
```

## Transactions

All service methods must be annotated with `@Transactional` (import: `org.springframework.transaction.annotation.Transactional`). 
Read-only methods must use `@Transactional(readOnly = true)` — this is also a hint for DB connection pool routing (primary/replica).

**Critical proxy limitation**: `@Transactional` is applied via Spring proxy. Internal calls within the same class (e.g. `this.someMethod()`) bypass the proxy and will NOT be transactional, even if annotated. Only public interface methods get the transactional behavior. Annotate every method individually rather than relying on class-level annotations plus internal delegation.

## Controller cross-cutting concerns

### Shared model attributes
Use the anottation `@ControllerAdvice` for exposing `@ModelAttribute's across all the controllers.


## Spring AOP annotations enabled in WebConfig

- `@Async` — for fire-and-forget methods (e.g., sending emails). Methods must
  receive locale and user as arguments; do NOT access `LocaleContextHolder` or
  `SecurityContextHolder` inside an `@Async` method (different thread).
- `@Scheduled` — for recurring background tasks.
- `@Cacheable` — for caching expensive computations.

These work via proxies (same limitation as `@Transactional`): they only apply
to public interface methods called from outside the class.