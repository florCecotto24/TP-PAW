# Repository guidance

**`agents.md`** is the repository guidance for Cursor, Claude Code, and other coding agents. Edit this file only.

## Commands

```bash
# Build all modules
mvn clean install

# Run the web application
mvn jetty:run -pl webapp

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl persistence

# Run a single test class
mvn test -pl persistence -Dtest=ListingHibernateDaoTest
```

The app is served at `http://localhost:8080/` with context path **`/webapp`** (see `application/application.properties`), so routes look like `http://localhost:8080/webapp/home`.

## Building and running

### Prerequisites

- Java 21
- Maven
- PostgreSQL reachable with credentials in **`webapp/src/main/resources/application/application.properties`** (`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`). Optional overrides: `application-${spring.profiles.active}.properties` (see `@PropertySources` in `WebConfig`). Local development: use `application-local.properties` for a local PostgreSQL instance.

### Key commands

```text
mvn clean install
mvn jetty:run -pl webapp
mvn test
```

Base URL depends on `server.port` and `server.servlet.context-path` in `application.properties`.

### Data source and schema (JPA)

PostgreSQL **`DataSource`** is built in **`RydenDataSourceFactory`**: idempotent baseline **`classpath:db/ryden_baseline.sql`**, then **Flyway** on **`classpath:db/migration/`** (`V2__`, `V3__`, …).

Tests use **in-memory HSQLDB** via **`TestPersistenceConfig`** (`schema-hsqldb.sql` under `persistence/src/test/resources`) — no PostgreSQL required for tests.

At runtime, **`WebConfig#entityManagerFactory`** configures Hibernate with `hibernate.hbm2ddl.auto=update` for local convenience when `@Entity` / `@Column` evolve; production schema changes must still ship as **Flyway** migrations (`V<number>__<description>.sql`), not auto-DDL alone. Flyway flags in **`RydenDataSourceFactory`**: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

## Architecture

This is a **car rental platform**: a Java web app on **Spring Framework 5.3**, multi-module **Maven**, strict layer separation.

### Module dependency chain

```
webapp → services → persistence → models
              ↑            ↑
   service-contracts  persistence-contracts
```

### Module responsibilities

- **models** — Organised by *feature/role* rather than by type:
  - `domain/` — JPA entities (`@Entity`): `Car`, `User`, `Listing`, `CarAvailability`, `Reservation`, `Image`, `CarPicture`, `StoredFile`, `AvailabilityPeriod`, etc.
  - `dto/` — transport objects grouped by business domain: `dto/car/` (cards, projections, market insight + `dto/car/detail/` for the public car page), `dto/listing/` (editor page model), `dto/profile/` (counterparty + profile page models), `dto/reservation/` (cards, page models, message DTOs). `dto/Page<T>` is the only generic that stays in the root.
  - `email/` — typed mail payloads consumed by `services/mail/`.
  - `pagination/` — `Page` slice (`SingleLayerPageWindow`) and `UiPaging`. UI page sizes live in `AppPaginationProperties` (webapp); DAOs paginate in SQL with `LIMIT`/`OFFSET` from those sizes. Services do not read pagination policy.
  - `security/` — `UserRole`.
  - `util/` — split by concern: `util/time/` (`AppTimezone`, wall-clock parsing/format, bookable calendar), `util/search/` (search criteria + sort sanitisers), `util/format/` (money, email normalisation), `util/rules/` (CBU, supported locales), `util/media/` (gallery + chat attachment content types). Business timezone single source of truth is `util/time/AppTimezone.WALL_ZONE`.
- **persistence-contracts** — DAO interfaces (`ListingDao`, `ReservationDao`, …).
- **persistence** — Hibernate/JPA DAOs under `persistence/hibernate/` (`*HibernateDao`). Tests run against HSQLDB. The legacy `persistence/jdbc/` tree remains as **reference** (typically not registered as Spring beans when Hibernate is active).
- **service-contracts** — Service interfaces, shared exceptions, `MessageKeys` for i18n codes.
- **services** — Business logic, email, async mail tasks.
- **webapp** — Spring MVC controllers, JSPs, `application/application.properties`, static assets, Thymeleaf mail under `classpath:mail/`.

### Key conventions

- **Dependency injection**: Constructor injection with `@Autowired`.
- **Persistence**: JPA via Hibernate. DAOs use `@PersistenceContext EntityManager em` with `em.find`, `em.persist`, JQL (`em.createQuery("FROM …", …)`), and **`em.createNativeQuery`** for DTO projections (`Object[]`) and read-only reporting. For **writable** work, prefer mutating **managed entities** (dirty checking at flush) over bulk JPQL `UPDATE`/`DELETE` or native `executeUpdate()` on entity-mapped tables unless the PR documents a justified exception. Prefer **named parameters** (`:name`) in native SQL for clarity and safe binding. `*HibernateDao` classes are `@Repository` and `@Transactional` where appropriate; `EntityManagerFactory` and `JpaTransactionManager` are wired in `WebConfig` (runtime) and `TestPersistenceConfig` (tests). Entities are scanned from `ar.edu.itba.paw.models`.
- **Entity mapping**: Foreign keys are often modeled as scalar `long` fields (`listingId`, `riderId`, …) with explicit JQL joins (`FROM Reservation r, Listing l WHERE l.id = r.listingId …`). If you add `@ManyToOne` / `@OneToMany`, prefer `FetchType.LAZY` and consider `OpenEntityManagerInViewFilter` for JSP navigation.
- **Enum persistence**: taxonomy enums (`Car.Type`, …) may use `@Enumerated(EnumType.STRING)`; lifecycle enums (`Listing.Status`, `Reservation.Status`) use **`AttributeConverter`** implementations that persist **lowercase** names to match legacy SQL and `LOWER(status)` filters.
- **IDs**: sequences use `@GeneratedValue(strategy = GenerationType.SEQUENCE, …)` with `@SequenceGenerator(…, allocationSize = 1)` aligned with PostgreSQL sequences.
- **Pagination**: paginate in SQL (`LIMIT`/`OFFSET` + separate `COUNT`) — never overfetch and slice in memory. For entity queries with joins, avoid relying on `setFirstResult` / `setMaxResults` alone on large sets — prefer ID-page + `IN` fetch or DTO-native queries.
- **Configuration**: Java `@Configuration` (`WebConfig`, `SpringMailConfig`, `WebAuthConfig`, `ValidationWebConfig`) plus `web.xml` for servlet bootstrap. Properties from `application.properties`; profile overrides from `application-{profile}.properties` if present.
- **Dependency versions**: Root `pom.xml` `<dependencyManagement>`; child POMs omit versions where managed. JPA stack: Hibernate 5.6.x, `javax.persistence-api` 2.2, `spring-orm` aligned with Spring 5.3.
- **Views**: JSPs in `webapp/src/main/webapp/WEB-INF/views/`, tags in `WEB-INF/tags/`, assets under `css/`, `js/`, `assets/`.
- **Component scan** (`WebConfig`): `ar.edu.itba.paw.webapp.controller`, `.advice`, `.exception`, `.util`, `.support`, `.security`, `.validation`, `.interceptor`, plus `ar.edu.itba.paw.services` and `ar.edu.itba.paw.persistence`. `webapp.form` and `webapp.dto` are not scanned; `CurrentUserArgumentResolver` in `webapp.support` is registered on the MVC adapter. Servlet listeners (e.g. `webapp.listener`) are declared in `WEB-INF/web.xml`.

### Domain overview

A `User` owns `Car`s. A `Car` can have a `Listing` with price and availability (`car_availability`). Other users create `Reservation`s. `Image`s are stored as byte arrays and linked via `CarPicture`.

Key enums (on model classes): `Car.Type`, `Car.Powertrain`, `Car.Transmission`, `Listing.Status` (active/paused/finished), `Reservation.Status` (pending, accepted, started, participant/automated cancellation variants, finished — see `Reservation.Status` in code).

## Technologies

- **Backend**: Java 21, Spring 5.3 (MVC, JDBC, Context, TX, ORM, Context Support for mail), Spring Security 5.8.10.
- **Database**: PostgreSQL (runtime), HSQLDB (DAO / persistence tests). Baseline **`db/ryden_baseline.sql`** + **Flyway** (`V2__`, `V3__`, … under `classpath:db/migration/`).
- **ORM**: Hibernate / JPA (`EntityManager`, `LocalContainerEntityManagerFactoryBean` in `WebConfig`).
- **Web UI**: JSP, Spring form tags, custom JSP tags under `WEB-INF/tags/`.
- **Client scripts**: Shared JS in `webapp/src/main/webapp/js/` (e.g. **Flatpickr** for date/range pickers, CDN in `header.jsp` / `footer.jsp`).
- **Email**: JavaMail, **Thymeleaf** HTML (`webapp/src/main/resources/mail/html/`), separate `ResourceBundleMessageSource` for mail copy (`mail/MailMessages` + locale variants).
- **Build**: Maven. **Runtime**: Jetty (`jetty-maven-plugin` on the `webapp` module).

## Database

Credentials live in **`webapp/src/main/resources/application/application.properties`**. **`RydenDataSourceFactory`** builds the PostgreSQL `DataSource`: it runs the idempotent baseline **`classpath:db/ryden_baseline.sql`**, then **Flyway** on **`classpath:db/migration`** (files under `webapp/src/main/resources/db/migration/`).

Tests use **in-memory HSQLDB** via `TestPersistenceConfig` (`schema-hsqldb.sql` in `persistence/src/test/resources`) — no PostgreSQL required for tests. Where useful, tests combine `JdbcTemplate` and the test `EntityManagerFactory` on the same `DataSource` to assert persisted state independently of the DAO under test.

### Flyway migrations

Flyway is configured in **`RydenDataSourceFactory`**: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

New migrations: `V<number>__<description>.sql` (e.g. `V4__add_reviews.sql`). Examples: `V2__users_extend_profile_and_auth.sql`, `V3__email_verification_codes.sql`.

## Internationalization (i18n)

- **UI/errors**: `ReloadableResourceBundleMessageSource` with `classpath:messages` and `classpath:exception-messages`. Default **English**; Spanish: `messages_es.properties`, `exception-messages_es.properties`.
- **Locale**: `AcceptHeaderLocaleResolver` in `WebConfig` — `Accept-Language`; supported **English** and **Spanish (`es`)**; defaults to English.
- **`LocaleMessages`** (`webapp.util`): resolves keys via `MessageSource` + `LocaleContextHolder`.
- **Mail**: `mail/MailMessages.properties` and `mail/MailMessages_es.properties`. `@Async` mail must not rely on `LocaleContextHolder` — capture locale on the request thread (e.g. `ReservationMailPayload` / related payload getters).
- **Exception keys**: `exception-messages.properties`, aligned with `ar.edu.itba.paw.exception.MessageKeys`.

## Email

- Configured in `SpringMailConfig` (`mail/emailconfig.properties`, `mail/javamail.properties`).
- HTML templates use keys such as `mail.reservationConfirmation.*`.
- Async sending uses `mailTaskExecutor` from `WebConfig`.

## Security

Configured in **`WebAuthConfig`**: Spring Security 5.7.14, `@EnableWebSecurity`, `SecurityFilterChain`, `RydenAuthenticationProvider`, `RydenUserDetailsService`. Remember-me, session auth, CSRF. **Do not** add or replace auth configuration outside `WebAuthConfig`.

## Dates and business rules (high level)

- Listing availability and reservation datetimes use wall zone `AppTimezone.WALL_ZONE` (Argentina) when parsing server-side. `AppTimezone` (in `models/util/`) is the single source of truth — never hardcode `ZoneId.of("America/Argentina/Buenos_Aires")` again.
- **Publishing**: valid date order; period start not before **today** in that zone (`ListingServiceImpl`).
- **Reserving**: pickup day not before **today**; interval must fit published availability (`ReservationServiceImpl`).
- **Flatpickr**: `minDate: 'today'` in `components.js` complements server validation.

## Development conventions

### Coding style

- **DI**: Constructor `@Autowired` as in existing code.
- **Service ↔ persistence**: Each `*ServiceImpl` injects its own DAOs. Cross-aggregate access goes through peer services (`UserService`, `ReservationService`, …). Use `@Lazy` on one constructor param when two services need each other to break cycles.
- **Scheduling** (`services/.../scheduling`): `@Scheduled` beans call services only, not DAOs.
- **Validation**: `ValidationWebConfig` registers `LocalValidatorFactoryBean` as the MVC validator.
- **Javadoc**: Public contracts in `*-contracts` in **English**. Avoid HTML `<p>` in Javadoc; use extra `*` lines or `{@code}` / `{@link}`. Controllers: short **English** class summary where it helps.

### Quality and security (recurring audit)

- **Spring versions**: Root `pom.xml` (`spring.version`, `spring-security.version`).
- **Controllers**: Call **services** only, not DAOs. No embedded business rules or direct mail sends; use service APIs. Prefer constructor injection and minimal visibility.
- **Exceptions & UX**: Domain failures → `RydenException` + message keys; `UnhandledExceptionHandler` must not expose raw `Throwable#getMessage()` (i18n keys; escape dynamic text with JSTL `c:out` when shown).
- **SQL / queries**: Use parameterized JQL or native SQL with **bound parameters** (named or positional); never concatenate user input into queries.
- **N+1 queries (strictly prohibited)**: Never return a `List<entity>` from a DAO and let callers trigger per-row lazy loads on LAZY associations. DAO methods that return a list of entities must pre-hydrate every association the known caller chain will navigate — typically via `JOIN FETCH` in JPQL, or ID-page + `IN` fetch / DTO-native projections (see **Pagination**). FK-only convenience accessors like `Reservation#getCarId()` / `#getRiderId()` are safe and need no fetch; property navigation across a non-FK association (`car.getOwner()`, `carModel.getBrand()`, …) is not. The fix belongs in the DAO; services and controllers must not work around it. Existing pattern examples: `findReservationsWithOverdueRefundProof`, `findReservationsRequiringRefundProofForOwner`, and `loadReservationCardsByIdNativeQuery` in `ReservationJpaDao`.
- **Logging**: Production `logback/logback-prod.xml` (typically **INFO+** for `ar.edu.itba.paw`); local may use `logback-local.xml` with **DEBUG** or **TRACE** on persistence packages when diagnosing listing/reservation SQL. Use **SLF4J 2** with parameterized messages — never string concatenation. **DEBUG** must use the fluent API (`LOGGER.atDebug()…log()`), not `LOGGER.debug(…)`; use `.setCause(throwable)` when the catch block swallows an exception but still needs traceability. **INFO** / **WARN** / **ERROR** use the matching fluent helpers (`atInfo`, `atWarn`, `atError`) for the same style.
- **Tests**: Arrange / Act / assert outcomes, not wiring. **No `Mockito.verify`** (or call-count tricks). Skip tests that only mirror a one-line delegate.
- **Style**: `final` where appropriate, private constructors on utility classes, immutable DTOs/criteria where practical; avoid magic numbers — read limits from `application.properties` (documented JVM fallbacks only where needed, e.g. `AppPaginationProperties`).
- **Constructors (Effective Java)**: At most **one public constructor** per class. Do not overload constructors — use **static factories** (`of`, `forUpload`, `denied`, `wrapping`, …) or a **builder** when several creation shapes exist. JPA entities may keep a **package-private** no-arg constructor for Hibernate plus a single public constructor (or static factory) for application code. Spring beans: one `@Autowired` constructor only.
- **Comments**: English only; remove obsolete chatty notes.

### Dependency management

- Versions in root `pom.xml` `<dependencyManagement>`; module POMs omit repeated versions.
- Internal modules: `${project.version}` for siblings.

### Testing

- **Test method names**: **`testFunctionName`** — camelCase, must **start with `test`**, then descriptive tail (e.g. `testRegistersUser()`). No `should…`, BDD prose, snake_case, or names without the **`test`** prefix.
- **One behavior per test**: Prefer a single clear behavior per `@Test`; when both return value and DB state matter, split into focused tests rather than asserting everything in one method.
- **Unit tests**: JUnit 5 + Mockito (`services`, `models`).
- **Persistence**: HSQLDB, DAO-style tests under `persistence/src/test`.
- **DAO integration tests** (`*HibernateDaoTest`, `*JdbcDaoTest`, `DaoIntegrationTestSupport`): After a **write** (`create*`, `update*`, `delete*`), call **`em.flush()`** when using JPA in the same transaction, then assert with **`JdbcTemplate`** / SQL fixtures — **never** verify persistence only via another method on the **same** DAO (e.g. `createCar` + `getCarById`) and avoid asserting writes solely with **`em.find`** (first-level cache). For ordered reads with fixtures, optional ground-truth `ORDER BY` via `JdbcTemplate`.
- **No interaction verification**: No `verify`, `verifyNoInteractions`, `never()`, `times()`, `InOrder`, captors for call wiring.
- **Do not emulate verify**: No counters or collector lists whose sole purpose is call counts.
- **Matchers**: Do not use `Mockito.anyLong()` (or `any*()`) as the **value** inside `when(...)` — use fixed literals.
- **Strict Mockito**: Remove unused stubbings (`UnnecessaryStubbingException`).

#### Frontend (`frontend/`, Vitest)

- **Solo contrato**: tests en `*.contract.test.ts` — alineación con `openapi.yaml`, hipermedia (`Link`, `X-Total-Count`, resolución de URNs `/webapp/api`), multipart y paridad i18n `es`/`en`. Sin tests de lógica UI, routing ni validación cliente aislada.
- **Unitarios**: sin red real; `fetch` mockeado cuando hace falta.
- **AAA**: cada `it` con `// 1.Arrange`, `// 2.Act`, `// 3.Assert` (o `// 2.Act / 3.Assert` cuando aplique).
- **Nombres**: prefijo `test` + comportamiento (`testParseLinkHeaderReadsNextAndLastRels`).

### Message keys

Domain / validation exception copy: **`exception-messages.properties`** (+ `_es`), consistent with **`MessageKeys`**.

### Application properties

- **Main**: `webapp/src/main/resources/application/application.properties` — port, context path (`/webapp`), uploads, validation, pagination, reservation timing, `app.scheduler.*` crons/zones.
- **Profiles**: `application-local.properties`, `application-deployed.properties` (examples in folder); secrets not committed.
- **Mail**: `mail/emailconfig.properties`, `mail/javamail.properties` under `webapp/src/main/resources/mail/`.

### Directory structure (per module)

- `src/main/java`, `src/main/resources`, `src/test/java` as usual.
- `webapp/src/main/webapp`: JSPs, CSS, JS, `WEB-INF/web.xml`.

### Key services and DAOs (orientation)

- **User**: `UserService` / `UserDao`.
- **Car & listing**: `CarService` / `CarDao`, `ListingService` / `ListingDao`.
- **Reservation**: `ReservationService` / `ReservationDao`.
- **Email & verification**: `EmailService`, `EmailVerificationService`, `PasswordResetService` and related DAOs.
- **Images**: `ImageService` / `ImageDao`, `CarPictureService` / `CarPictureDao`.
- **Session**: e.g. `PublishCarStashSessionListener` for publish-form stash cleanup.

## Logging

Use SLF4J 2 (not Log4j or `java.util.logging`):

```java
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
```

**Always use the fluent API** — do not call `LOGGER.debug(…)`, `LOGGER.info(…)`, etc.:

```java
// DEBUG — routine trace, expected no-ops, swallowed parse/IO fallbacks
LOGGER.atDebug().addArgument(userId).log("Public verification-code request for unknown user id={} (silent no-op)");
LOGGER.atDebug().setCause(ex).log("Reservation max-billable-days check skipped: unparseable wall datetimes");

// INFO / WARN / ERROR — same pattern with atInfo / atWarn / atError
LOGGER.atInfo().addArgument(user.getId()).log("Email verified for user id={}");
LOGGER.atWarn().setCause(e).addArgument(to).log("Failed to send email to {}");
```

Rules:

- Placeholders `{}` in `.setMessage(…)` / `.log(…)`; bind values with `.addArgument(…)` — never concatenate into the message string.
- When a `catch` block continues after logging, attach the throwable with `.setCause(ex)` so the stack trace is preserved.
- `.log()` at the end actually emits the event; nothing is written before that call.

## Transactions

All service methods need `@Transactional` (`org.springframework.transaction.annotation.Transactional`). Read-only methods: `@Transactional(readOnly = true)` (pool hint for primary/replica).

With JPA, transactional boundaries also control **dirty checking** and **auto-flush**: a `@Transactional(readOnly = true)` method must not rely on persisted mutations from that method — use a writable transaction when the entity state should commit.

**Dirty checking for writable transactions**: Any Spring `@Transactional` method that is **not** `readOnly = true` and that **writes or updates JPA-mapped domain rows** must persist those changes through Hibernate **dirty checking** (or entity lifecycle): load managed entities (`em.find`, navigable associations, or results of managed queries), mutate fields or collections (or use `em.persist` / `em.remove` for inserts and deletes), and rely on flush at commit. Do **not** use bulk JPQL `UPDATE`/`DELETE` or native `executeUpdate()` against entity-backed tables as the default way to change domain state; deviations require an explicit **PR justification**. Service beans stay free of `EntityManager` — they satisfy this rule by delegating to DAOs whose writable operations follow it. Methods with writable `@Transactional` that perform **no JPA persistence** (e.g. mail-only `@Async` senders) are exempt.

**Proxy limitation**: Internal `this.someMethod()` calls skip the proxy and ignore `@Transactional`. Only external calls through the proxy apply. Annotate public service methods individually; do not rely on class-level + internal delegation.

### PR checks (transactionality)

- Every **public** method in `services/.../*ServiceImpl.java` must have explicit `@Transactional`.
- Pure reads / normalization: `readOnly = true`.
- Writes, side effects, mail orchestration: `@Transactional` without `readOnly = true`.
- Writable transactions that touch JPA domain state: follow **dirty checking** (see **Dirty checking for writable transactions** above); DAO implementations must not silently reintroduce bulk DML for those rows without PR justification.
- Private helpers are not transactional entry points.
- Any public method **without** `@Transactional` needs an explicit PR justification.

Quick checks:

```text
rg "public .*\\(" services/src/main/java/ar/edu/itba/paw/services | rg "ServiceImpl"
rg "@Transactional\\(readOnly\\s*=\\s*true\\)|@Transactional" services/src/main/java/ar/edu/itba/paw/services
rg "this\\.[a-zA-Z0-9_]+\\(" services/src/main/java/ar/edu/itba/paw/services
```

## Controller cross-cutting concerns

### Shared model attributes

Use `@ControllerAdvice` to expose `@ModelAttribute` beans across controllers.

### REST URL routing

- **Resource identity on GET**: Identifiers that name the resource being accessed (`carId`, `userId`, `reservationId`, `documentType`, …) belong in the path as `@PathVariable`s — e.g. `GET /cars/{carId}`, `GET /users/{userId}/profile`, `GET /profile/documents/{documentType}` — not as `@RequestParam` query parameters. Prefer `/cars/123` over `/car-detail?carId=123`.
- **Query params for everything else**: Pagination, sorting, filters, and UI/breadcrumb state (`page`, `sort`, `status`, `role`, `src`, `fromCar`, `tab`, …) stay as query params.
- **Consistency**: New controllers, JSP/tag links, mail CTA URLs, JS fetch endpoints, and `redirect:` strings must follow the same shape. Canonical examples already in the app: `/my-cars/car/{carId}`, `/my-reservations/{reservationId}`.

### Hypermedia collections (embedded teasers vs link-only)

Reference: `openapi.yaml` `info.description`.

#### What cátedra corrections actually penalize

- Embedding **another aggregate’s resources** inside a parent (e.g. `user` JSON with a `cars[]` array) — breaks canonical URN and ACL (LINEAMIENTOS §1.5).
- **Empty or unused** `links.*` on DTOs while the client hardcodes URL shapes (Correcciones PAW: *error grave*).
- Passing **bare IDs** in bodies where the API should speak in **URNs** (`productId`, `documentId` in PATCH — Dic2025 corrections).
- **Client-side URL construction** from IDs instead of following `links.self`.

Not the same as: a **paginated primary collection** (`GET /cars`) returning teasers with `links.self` on each row (LINEAMIENTOS §1.6).

#### Two kinds of “N+1”

| Kind | Rule |
|------|------|
| **Persistence N+1** | **Prohibited** — fix in DAO (`JOIN FETCH`, ID-page + `IN`, native DTO). See **N+1 queries** above. |
| **HTTP follow N+1** | **Sometimes intentional** — link-only collection + one `GET` per `links.self`. Trade REST purity vs round-trips. Document in OpenAPI. |

#### Decision checklist for new collections

1. **Does each member already have a canonical URN elsewhere?** (car → `/cars/{id}`, review → `/reviews/{id}`)  
   - Small/bounded set (similar, favorites, …) → **link-only** (pattern A).  
   - Large paginated grid where this URI *is* the main entry → **embedded teasers** with `links.self` (pattern B).

2. **Is the collection the natural home of the sub-resource?** (pictures under car, messages under reservation) → **embedded** OK if each item has `links.self`.

3. **Are items values without their own resource URI?** (bookable segments) → **embedded** only; no link-only migration.

4. **Never** return bare IDs in the collection body — only `Links` (`self` URI) or DTOs that include `links.self`.

#### Pattern A — link-only (intentional HTTP N+1)

Collection MIME returns `array` of `Links`. Client `Accept`s item MIME on each `self`.

| Endpoint | Collection MIME | Follow with |
|----------|-------------------|-------------|
| `GET /cars/{id}/similar` | `car.similar.v1+json` | `car.summary.v1+json` |
| `GET /users/{id}/favorites` | `user.favorites.v1+json` | `car.summary.v1+json` |
| `GET /reviews` | `review.links.v1+json` | `review.v1+json` |
| `GET /reservations` | `reservation.links.v1+json` | `reservation.summary.v1+json` |

Frontend helper: `frontend/src/api/linkCollection.ts` (`followLinkCollection`, `getLinkCollectionPage`).

#### Pattern B — embedded (documented tradeoff)

Body returns DTOs; **each item must include `links.self`** pointing at the canonical item URI. Defensible in oral/defensa when the collection is primary, sub-resource, catalog, or computed values.

| Endpoint | MIME | Why embedded |
|----------|------|--------------|
| `GET /cars` | `car.summary` / `car` | Primary browse/search collection (LINEAMIENTOS §1.6); paginated grids. |
| `GET /cars/{id}/pictures` | `picture` | Gallery sub-resource; metadata + pagination in one response. |
| `GET /cars/{id}/availabilities` | `availability` | Owner/rider calendar view; dates/prices needed together. |
| `GET /cars/{id}/bookable-segments` | `bookablesegment` | Computed segments; no per-item REST URI. |
| `GET /reservations/{id}/messages` | `message` | Chat/polling; body needed in list. |
| `GET /brands`, `/models`, `/neighborhoods` | catalog MIMEs | Small reference catalogues. |
| `GET /users` (admin) | `user.private` | Admin roster of the collection itself. |

When adding a collection of **existing canonical resources** referenced from another aggregate, default to **pattern A**. Use **pattern B** only when one of the “why embedded” rows above applies and OpenAPI documents it.

## Spring AOP (enabled in `WebConfig`)

- **`@Async`**: fire-and-forget (e.g. mail). Pass locale/user as arguments; **do not** use `LocaleContextHolder` or `SecurityContextHolder` inside `@Async` (different thread).
- **`@Scheduled`**: recurring jobs.
- **`@Cacheable`**: cache expensive work.

Same proxy rule as `@Transactional`: only applies to **public** methods invoked from outside the bean.

## UI / Visual design conventions

### Background contrast rule

The page body uses Bootstrap's `bg-light` (`#f8f9fa`, "cream/off-white"). Any section or card rendered directly on this background **must** carry `bg-white` so it visually pops. Nested elements placed *inside* a `bg-white` section that need their own background should use `bg-light` (the cream tone) to keep the alternating contrast going.

Quick checklist when adding a new section card:
- Section sits on `bg-light` page → add `bg-white` to the card/container.
- Sub-element sits on `bg-white` card → use `bg-light` for its background.

This applies to JSP fragments, tag files, and any inline HTML added to existing views.
