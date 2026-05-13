# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

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
mvn test -pl persistence -Dtest=ListingHibernateDaoTest
```

The app is served at `http://localhost:8080/` (context path is `/webapp` per `application.properties`, so routes like `/webapp/home`).

## Database

Database credentials are in `webapp/src/main/resources/application.properties`. The PostgreSQL `DataSource` is built in `RydenDataSourceFactory`: it applies the idempotent baseline `classpath:db/ryden_baseline.sql` first, then runs Flyway over `classpath:db/migration/`.

Tests use an **in-memory HSQLDB** database configured in each module's `TestPersistenceConfig.java` (loads `schema-hsqldb.sql`) — no PostgreSQL required for testing.

### Schema management with JPA

Schema in production comes from the baseline SQL + Flyway migrations (above). At runtime Hibernate is configured with `hibernate.hbm2ddl.auto=update` in `WebConfig#entityManagerFactory` — useful in dev to pick up new `@Entity`/`@Column` definitions, but the source of truth for production is still the Flyway migrations, not Hibernate's auto-DDL. Any schema change must ship as a Flyway migration.

### Flyway migrations

New migrations must follow the naming convention `V<number>__<description>.sql` (e.g. `V4__add_reviews.sql`). Flyway config in `RydenDataSourceFactory`: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

## Architecture

This is a **car rental platform** built as a multi-module Maven project with strict layer separation.

### Module dependency chain

```
webapp → services → persistence → models
              ↑            ↑
   service-contracts  persistence-contracts
```

- **models** — JPA entities (`@Entity` annotated): `Car`, `User`, `Listing`, `ListingAvailability`, `Reservation`, `Image`, `CarPicture`, `StoredFile`, plus DTOs `ListingCard`, `ListingDetail`, `Page`, etc.
- **persistence-contracts** — DAO interfaces (e.g., `ListingDao`, `CarDao`).
- **persistence** — Hibernate/JPA implementations under `persistence/hibernate/` (e.g., `ListingHibernateDao`). DAO tests live here and run against HSQLDB. The legacy `persistence/jdbc/` package is **kept as reference only** — those classes have `@Repository` removed and are no longer Spring beans.
- **service-contracts** — Service interfaces (e.g., `ListingService`, `ReservationService`).
- **services** — Service implementations containing business logic. Service tests mock DAOs with Mockito.
- **webapp** — Spring MVC controllers, JSP views, and all Spring configuration.

### Key conventions

- **Dependency injection**: Constructor injection with `@Autowired`.
- **Persistence**: JPA via Hibernate. DAOs inject `@PersistenceContext EntityManager em` and use:
  - `em.find(Entity.class, id)` for primary-key reads,
  - `em.persist(entity)` for inserts,
  - JQL (`em.createQuery("FROM Entity e WHERE …", Entity.class)`) for queries by attribute,
  - `em.createNativeQuery(...)` only when projecting DTOs (`Object[]`) or doing bulk updates that don't fit cleanly in JQL.
  - All `*HibernateDao` classes are annotated `@Repository` + `@Transactional`. The `EntityManagerFactory` and `JpaTransactionManager` are wired in `WebConfig#entityManagerFactory` (production) and `TestPersistenceConfig` (HSQLDB tests). Entities are scanned from `ar.edu.itba.paw.models`.
- **Entity mapping**: FKs are currently modeled as raw `long ownerId` / `long carId` / `long listingId` fields on the entities (not as `@ManyToOne` associations). JQL joins are explicit (`FROM Reservation r, Listing l WHERE l.id = r.listingId AND …`). If you introduce true associations (`@ManyToOne` / `@OneToMany`), default to `fetch = FetchType.LAZY` and add an `OpenEntityManagerInViewFilter` so JSPs can navigate lazy collections.
- **Enum persistence**:
  - Stateless taxonomy enums (`Car.Type`, `Car.Powertrain`, `Car.Transmission`) use `@Enumerated(EnumType.STRING)` — uppercase in the DB.
  - Status enums (`Listing.Status`, `Reservation.Status`) use a JPA `AttributeConverter` (`StatusConverter`) that stores the name in **lowercase** to match the legacy schema and the existing native-SQL filters (`WHERE LOWER(r.status) = 'pending'`). Do not switch these to `@Enumerated(EnumType.STRING)` without a migration to uppercase column values.
- **IDs**: every entity uses `@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "…_id_seq")` paired with a `@SequenceGenerator(allocationSize = 1)`. Keep `allocationSize = 1` for compatibility with the existing PostgreSQL sequences.
- **Pagination**: when paginating an **entity** query that contains a `JOIN`, do **not** rely on `setFirstResult` / `setMaxResults` alone — Hibernate will materialize the whole result set and slice in memory. Use the **1+1 queries** pattern (page IDs via a `SELECT id … LIMIT/OFFSET` native or scalar query, then `FROM Entity e WHERE e.id IN :ids`). Most paginated reads in this project already sidestep the issue by returning **DTOs** (`ListingCard`, `ReservationCard`) via `Object[]` projections instead of entities. For the DB-fetch-size > UI-page-size pattern, see `DualLayerPageWindow`.
- **Dirty checking**: write transactions auto-flush modified managed entities. Do not rely on it implicitly — prefer explicit `em.persist` / explicit `UPDATE` JQL — but be aware that mutating an entity loaded inside a `@Transactional` write method will be persisted on commit even without an explicit save call.
- **Configuration**: Java-based (`WebConfig.java`, `SpringMailConfig.java`) bootstrapped from `web.xml`. Properties loaded from `application.properties`; profile-specific overrides load from `application-{profile}.properties` if present.
- **Dependency versions**: All versions declared in root `pom.xml` `<dependencyManagement>`. Child modules reference dependencies without versions. JPA/Hibernate-relevant versions: `org.hibernate.version=5.6.15.Final`, `javax.persistence-api=2.2`, `spring-orm=${spring.version}` (5.3.33).
- **Views**: JSPs in `webapp/src/main/webapp/WEB-INF/views/`. Reusable tag files in `WEB-INF/tags/`. Static assets (CSS, JS, images) served from `webapp/src/main/webapp/css/`, `/js/`, `/assets/`.
- **Component scan**: `WebConfig` scans `ar.edu.itba.paw.webapp.controller`, `ar.edu.itba.paw.webapp.advice`, `ar.edu.itba.paw.webapp.exception`, `ar.edu.itba.paw.webapp.util`, `ar.edu.itba.paw.webapp.support`, `ar.edu.itba.paw.webapp.security`, `ar.edu.itba.paw.webapp.validation`, `ar.edu.itba.paw.webapp.interceptor`, `ar.edu.itba.paw.services`, and `ar.edu.itba.paw.persistence` (which picks up the Hibernate DAOs).

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

- **Test method naming**: Every `@Test` method must be named **`testFunctionName`**: camelCase beginning with **`test`**, then a descriptive tail (e.g. `testRegistersUser()`, `testDaoWriteVisibleInJdbcTemplate()`). Do not use `should…`, BDD prose, snake_case, or names without the **`test`** prefix.
- **One behavior per test**: each `@Test` exercises the class under test **once** and asserts one behavior. Splitting "returns the created entity" and "persists to the DB" into two methods is mandatory, not optional — do not combine a return-value assertion with a DB-state assertion in the same test.
- **Unit tests**: JUnit 5 + Mockito in `services` and `models`.
- **Persistence tests**: HSQLDB (`TestPersistenceConfig`), integration-style DAO tests under `persistence/src/test`. The test config wires both a `JdbcTemplate` and a `LocalContainerEntityManagerFactoryBean` over the same embedded HSQLDB `DataSource`, so tests can assert against the DB independently of the DAO under test.
- **DAO integration tests** (`*HibernateDaoTest`, `DaoIntegrationTestSupport`): After a **write** (`create*`, `update*`, `delete*`), call `em.flush()` to force the SQL out, then assert with **`JdbcTemplate`** / `JdbcTestUtils` or SQL fixtures in arrange — **never** confirm persistence via a **second method on the same DAO** (e.g. `createCar` + `getCarById`). Do not assert via `em.find(...)` either — that reads from the persistence context, not the DB. Prefer `INSERT` via `JdbcTemplate` or SQL fixtures for setup when testing updates or deletes on that DAO. For ordered **read** results backed by SQL fixtures, optionally assert ordering against a matching `ORDER BY` query via `JdbcTemplate`.
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

With JPA the boundary is doubly important: dirty checking and auto-flush only happen inside a transaction. A `@Transactional(readOnly = true)` method that mutates an entity will not have those changes persisted on commit — make the method writable if you intend the mutation to land in the DB.

**Critical proxy limitation**: `@Transactional` is applied via Spring proxy. Internal calls within the same class (e.g. `this.someMethod()`) bypass the proxy and will NOT be transactional, even if annotated. Only public interface methods get the transactional behavior. Annotate every method individually rather than relying on class-level annotations plus internal delegation.

### PR checks (transactionality)

- `services/src/main/java/.../*ServiceImpl.java`: every public method must have explicit `@Transactional`.
- Pure reads or normalization/build helpers that only read data must use `@Transactional(readOnly = true)`.
- Writes, state changes, and side effects (including mail dispatch orchestration) must use `@Transactional` without `readOnly = true`.
- Private helpers are not transactional entry points; the rule applies to public service methods.
- If a method in a `*ServiceImpl` is intentionally left without `@Transactional`, the PR must justify that exception explicitly.

Quick reviewer checks:

```text
rg "public .*\\(" services/src/main/java/ar/edu/itba/paw/services | rg "ServiceImpl"
rg "@Transactional\\(readOnly\\s*=\\s*true\\)|@Transactional" services/src/main/java/ar/edu/itba/paw/services
rg "this\\.[a-zA-Z0-9_]+\\(" services/src/main/java/ar/edu/itba/paw/services
```

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
