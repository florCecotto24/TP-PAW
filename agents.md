# Project Overview

This is a Java web application based on **Spring Framework 5.3**, organized as a multi-module **Maven** project. It uses a classic layered architecture with separated contracts and implementations.

## Architecture

The project is divided into the following modules:

- **models**: Domain entities (POJOs) and shared utilities (e.g. wall-clock parsing, `AvailabilityPeriod` with `America/Argentina/Buenos_Aires`).
- **persistence-contracts**: DAO interfaces.
- **persistence**: JDBC implementations using `JdbcTemplate`.
- **service-contracts**: Service interfaces, shared exceptions, and `MessageKeys` for i18n codes.
- **services**: Service implementations (business logic, email, async mail task).
- **webapp**: Spring MVC controllers, JSP views, static assets, `application.properties`, and mail Thymeleaf templates under `classpath:mail/`.

## Technologies

- **Backend**: Java 21, Spring 5.3 (MVC, JDBC, Context, TX, Context Support for mail), Spring Security 5.7.14.
- **Database**: PostgreSQL (runtime), HSQLDB (DAO / persistence tests). Schema managed via **Flyway** (`V2__`, `V3__` migrations under `classpath:db/migration`).
- **Web UI**: JSP (`webapp/src/main/webapp/WEB-INF/views/`), Spring form tags, custom JSP tags under `WEB-INF/tags/`.
- **Client scripts**: Shared JS in `webapp/src/main/webapp/js/` (e.g. **Flatpickr** for date/range pickers, loaded from CDN in `header.jsp` / `footer.jsp`).
- **Email**: JavaMail, **Thymeleaf** HTML templates (`webapp/src/main/resources/mail/html/`), separate `ResourceBundleMessageSource` for mail copy (`mail/MailMessages` + locale variants).
- **Build**: Maven.
- **Runtime server**: Jetty (`jetty-maven-plugin` on the `webapp` module).

## Building and Running

### Prerequisites

- Java 21
- Maven
- PostgreSQL reachable with credentials defined in **`webapp/src/main/resources/application.properties`** (`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`). Optional overrides: `application-${spring.profiles.active}.properties` (see `@PropertySources` in `WebConfig`). Local development: use `application-local.properties` for a local PostgreSQL instance.

### Key commands

**Build the entire project:**

```text
mvn clean install
```

**Run the web application:**

```text
mvn jetty:run -pl webapp
```

Base URL depends on `server.port` and `server.servlet.context-path` in `application.properties` (if `context-path` is set, prefix all paths accordingly).

**Run tests:**

```text
mvn test
```

## Internationalization (i18n)

- **UI and errors**: `ReloadableResourceBundleMessageSource` with basenames `classpath:messages` and `classpath:exception-messages`. Default bundle is **English**; Spanish uses `messages_es.properties` and `exception-messages_es.properties`.
- **Locale resolution**: `AcceptHeaderLocaleResolver` in `WebConfig` — uses the browser **`Accept-Language`**; supported locales **English** and **Spanish (`es`)**; default **English** if no match.
- **`LocaleMessages`** (`webapp.util`): resolves exception / key messages via `MessageSource` and `LocaleContextHolder`.
- **Mail**: Separate bundle `mail/MailMessages.properties` (English default) and `mail/MailMessages_es.properties`. Reservation confirmation emails use the request locale captured on the **synchronous** thread (see `ReservationConfirmationPayload#getMessageLocale`) because `@Async` mail work does not inherit `LocaleContextHolder`.

## Email

- Configured in `SpringMailConfig` (`mail/emailconfig.properties`, `mail/javamail.properties`).
- HTML templates reference message keys such as `mail.reservationConfirmation.*`.
- Async sending uses `mailTaskExecutor` from `WebConfig`.

## Dates and business rules (high level)

- Listing availability and reservation form datetimes are interpreted in the **wall zone** `AvailabilityPeriod.WALL_ZONE` (Argentina) when parsing/normalizing server-side.
- **Publishing**: availability periods must have a valid date order; the **start** of each period must not be before **today** in that zone (see `ListingServiceImpl`).
- **Reserving**: pickup (wall calendar day) must not be before **today** in that zone; interval must still fit published availability (see `ReservationServiceImpl`).
- **Flatpickr**: range/single pickers default to **`minDate: 'today'`** in `components.js` so past calendar days are not selectable in the browser (complements server validation).

## Development conventions

### Coding style

- **Dependency injection**: Constructor injection with Spring `@Autowired` (as used in existing services/config).
- **Configuration**: Java `@Configuration` (`WebConfig`, `SpringMailConfig`, `WebAuthConfig`, `ValidationWebConfig`) plus `web.xml` for servlet bootstrap.
- **Security**: Configured in `WebAuthConfig` with Spring Security 5.7.14. Uses `@EnableWebSecurity`, `SecurityFilterChain`, custom `RydenAuthenticationProvider` and `RydenUserDetailsService`. Remember-me support, session-based auth, CSRF protection.
- **Validation**: `ValidationWebConfig` implements `WebMvcConfigurer` to inject `LocalValidatorFactoryBean` as the MVC validator. Bean validation and Spring form validation combined.

### Dependency management

- **Centralized versions**: Maven properties and `<dependencyManagement>` in the root `pom.xml`.
- **Module POMs**: External dependencies without repeating versions where managed by the parent.
- **Internal modules**: Use `${project.version}` for sibling artifacts.

### Testing

- **Unit tests**: JUnit 5 and Mockito (`services`, `models`).
- **Persistence tests**: HSQLDB (`TestPersistenceConfig`, DAO integration-style tests under `persistence/src/test`).
- **DAO integration tests (`*JdbcDaoTest`, `DaoIntegrationTestSupport`)**: After exercising a **write** on the DAO under test (`create*`, `update*`, `delete*`), assert persisted state with **`JdbcTemplate`** (injected on the support class) or **`insert*`** / raw SQL in arrange — **do not** verify the write by calling a **second method on the same DAO** (e.g. `createCar` then `getCarById`). Prefer SQL fixtures for arrange when the goal is to test updates or deletes, so setup does not go through unrelated create methods on that DAO. For **read** methods with SQL fixtures, one DAO call is fine; when the contract is **ordering**, consider aligning assertions with an explicit `ORDER BY` query via `JdbcTemplate` as ground truth.
- **Contract over implementation**: Tests assert **observable outcomes** (return values, `ModelAndView` / HTTP, domain state via test doubles or integration tests). **Do not** use `Mockito.verify`, `verifyNoInteractions`, `verifyNoMoreInteractions`, `never()`, `times()`, `InOrder`, or argument captors to assert that collaborators were called — that couples tests to internal wiring.
- **No `verify` emulation**: Do not emulate interaction verification with counters, call-collector lists, or stubs whose only purpose is asserting call count/order. If a test uses helpers such as `doAnswer`, assertions must target contract-level data/effects (e.g., produced payload/content), not mock wiring.
- **Service tests**: Mock DAOs and collaborators only to supply behavior; avoid `Mockito.anyLong()` (and similar) as **assigned values** — use fixed literals inside `when(...)`.
- **Strict Mockito**: Remove unused stubbings or they fail with `UnnecessaryStubbingException`.

### Message keys

- Domain / validation strings for exceptions: **`exception-messages.properties`** (and `_es`), keyed consistently with **`ar.edu.itba.paw.exception.MessageKeys`**.

### Application properties

- **Main config**: `webapp/src/main/resources/application.properties` defines database credentials, server port, context path (`/webapp`), upload limits, validation rules (password min-length, phone pattern), and mail links context.
- **Profile-specific**: `application-local.properties` for local PostgreSQL development (overrides `spring.datasource.*` settings).
- **Mail config**: `mail/emailconfig.properties` and `mail/javamail.properties` under `webapp/src/main/resources/mail/`.

### Flyway database migrations

- **Location**: `classpath:db/migration/` (under `webapp/src/main/resources/db/migration/`).
- **Schema bootstrap**: `schema.sql` loaded first via `DataSourceInitializer`, then Flyway migrations applied.
- **Flyway config** (in `WebConfig.java`): `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.
- **Existing migrations**: `V2__users_extend_profile_and_auth.sql`, `V3__email_verification_codes.sql`.
- **Note**: New migrations should follow naming convention `V<number>__<description>.sql`.

### Directory structure (per module)

- `src/main/java`: Java sources.
- `src/main/resources`: Config, SQL, i18n bundles, mail templates.
- `src/test/java`: Tests.
- `webapp/src/main/webapp`: JSPs, CSS, JS, `WEB-INF/web.xml`.
- **webapp component scan** (`WebConfig`): Covers `ar.edu.itba.paw.webapp.controller`, `ar.edu.itba.paw.webapp.util`, `ar.edu.itba.paw.webapp.security`, `ar.edu.itba.paw.webapp.validation`, plus `ar.edu.itba.paw.services` and `ar.edu.itba.paw.persistence`.

### Key services and DAOs

- **User**: `UserService` / `UserDao` — authentication, registration, profile management.
- **Car & Listing**: `CarService` / `CarDao`, `ListingService` / `ListingDao` — rental inventory.
- **Reservation**: `ReservationService` / `ReservationDao` — reservation management.
- **Email & verification**: `EmailService`, `EmailVerificationService` / `EmailVerificationCodeDao`, `PasswordResetService` / `PasswordResetCodeDao` — password reset flows, async email sending.
- **Image & picture**: `ImageService` / `ImageDao`, `CarPictureService` / `CarPictureDao` — image uploads and associations.
- **Session listener**: `PublishCarStashSessionListener` (in `webapp/src/main/java/ar/edu/itba/paw/webapp/listener/`) manages session state for car publish forms.

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