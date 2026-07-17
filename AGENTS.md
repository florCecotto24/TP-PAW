# Repository guidance

**`agents.md`** is the repository guidance for Cursor, Claude Code, and other coding agents. Edit this file only.

## Commands

```bash
# Build all modules (includes frontend npm build → WAR)
mvn clean install

# Run the web application (API + packaged SPA)
mvn compile -pl webapp -am
mvn jetty:run -pl webapp

# SPA dev server (proxies /api and /webapp/api → Jetty)
cd frontend && npm run dev

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl persistence
mvn test -pl frontend

# Run a single test class
mvn test -pl persistence -Dtest=CarJpaDaoTest

# Frontend Vitest only
cd frontend && npm test
```

The app is served at `http://localhost:8080/` with context path **`/webapp`** (see `application/application.properties`):

- **SPA UI**: `http://localhost:8080/webapp/...` (client routes)
- **REST API**: `http://localhost:8080/webapp/api/...`

On **Pampero** the public URL is **`…/paw-2026a-08/`**. Before uploading the WAR, build the SPA with `npm run build:pampero` (or `mvn … -Dpampero.spa=true`) so Vite `base` is `/paw-2026a-08/`.

During SPA development, Vite typically serves the UI on its own port with `base: '/'` and proxies API calls to Jetty.

## Building and running

### Prerequisites

- Java 21
- Maven
- PostgreSQL reachable with credentials in **`webapp/src/main/resources/application/application.properties`** (`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`). Optional overrides: `application-${spring.profiles.active}.properties` (see `@PropertySources` in `WebConfig`). Local development: use `application-local.properties` for a local PostgreSQL instance.
- **Node 20+** for local SPA work. Full Maven builds install Node via `frontend-maven-plugin`. For shell npm without a global Node, use `frontend/use-node.sh` to put the Maven-managed Node on `PATH`.

### Key commands

```text
mvn clean install
mvn compile -pl webapp -am
mvn jetty:run -pl webapp
cd frontend && npm run dev
mvn test
```

Base URL depends on `server.port` and `server.servlet.context-path` in `application.properties`.

**Jetty + SPA:** `jetty:run` serves `target/webapp-composite/` (static shell from `webapp/src/main/webapp/` merged with `frontend/dist/` from the last `npm run build`). Run `mvn compile -pl webapp -am` before Jetty so the frontend build is current; do not commit hashed bundles under `webapp/src/main/webapp/public/`.

### Data source and schema (JPA)

PostgreSQL **`DataSource`** is built in **`RydenDataSourceFactory`**: idempotent baseline **`classpath:db/ryden_baseline.sql`**, then **Flyway** on **`classpath:db/migration/`** (`V2__`, `V3__`, …).

Tests use **in-memory HSQLDB** via **`TestPersistenceConfig`** (`schema-hsqldb.sql` under `persistence/src/test/resources`) — no PostgreSQL required for tests.

At runtime, **`application.properties`** must keep `hibernate.hbm2ddl.auto=update` — **cátedra requirement** (do not change to `none` / `validate`). `WebConfig#entityManagerFactory` reads that property. Flyway still applies versioned migrations (`V<number>__<description>.sql`) via **`RydenDataSourceFactory`**; auto-DDL does not replace shipping migrations. Flyway flags: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

## Architecture

This is a **car rental platform**: multi-module **Maven**, strict layer separation. The browser UI is a **React SPA**; the server exposes a **Jersey/JAX-RS** hypermedia REST API. Spring Framework 5.3 provides DI, transactions, JPA, security, and mail — not MVC view rendering.

### Module dependency chain

```
frontend  →  (assets into WAR)  webapp → services → persistence → models
                                         ↑            ↑
                              service-contracts  persistence-contracts
```

`frontend` builds with `frontend-maven-plugin` before `webapp`; `maven-war-plugin` embeds `frontend/dist` into the WAR.

### Module responsibilities

- **models** — Organised by *feature/role* rather than by type:
  - `domain/` — JPA entities (`@Entity`): `Car`, `User`, `CarAvailability`, `Reservation`, `Image`, `CarPicture`, `StoredFile`, `AvailabilityPeriod`, etc.
  - `dto/` — transport / page-model objects grouped by business domain: `dto/car/` (cards, projections, market insight, gallery items), `dto/profile/`, `dto/reservation/` (cards, page models, message DTOs), `dto/file/` (`BinaryContent`). `dto/Page<T>` is the only generic that stays in the root. REST wire DTOs for the API live under **`webapp/.../dto/rest/`**, not here.
  - `email/` — typed mail payloads consumed by `services/mail/`.
  - `pagination/` — `Page` slice (`SingleLayerPageWindow`) and `UiPaging`. UI page sizes live in `AppPaginationProperties` (webapp); DAOs paginate in SQL with `LIMIT`/`OFFSET` from those sizes. Services do not read pagination policy.
  - `security/` — `UserRole`.
  - `util/` — split by concern: `util/time/` (`AppTimezone`, wall-clock parsing/format, bookable calendar), `util/search/` (search criteria + sort sanitisers), `util/format/` (money, email normalisation), `util/rules/` (CBU, supported locales), `util/media/` (gallery + chat attachment content types). Business timezone single source of truth is `util/time/AppTimezone.WALL_ZONE`.
- **persistence-contracts** — DAO interfaces (`CarDao`, `ReservationDao`, …).
- **persistence** — JPA DAOs under feature packages (`persistence/car/`, `persistence/reservation/`, …) as `*JpaDao`. Tests run against HSQLDB.
- **service-contracts** — Service interfaces, shared exceptions, `MessageKeys` for i18n codes.
- **services** — Business logic, email, async mail tasks, scheduling.
- **frontend** — Vite + React 18 + TypeScript SPA (`ryden-spa`). Feature-sliced UI under `frontend/src/features/`; hypermedia client under `frontend/src/api/`.
- **webapp** — Jersey JAX-RS resources (`controller/`, filter-mapped at `/api/*`), REST DTOs (`dto/rest/`), JWT security, `SpaFallbackFilter`, `application/application.properties`, static SPA shell merged at build time (`index.html` + `/public/` from `frontend/dist/`). Mail SMTP properties under `classpath:mail/config/`; HTML templates and `MailMessages` live in **`services`** (`classpath:mail/html/`, `classpath:mail/messages/`).

### Key conventions

- **Dependency injection**: Constructor injection with `@Autowired`.
- **Persistence**: JPA via Hibernate. DAOs use `@PersistenceContext EntityManager em` with `em.find`, `em.persist`, JQL (`em.createQuery("FROM …", …)`), and **`em.createNativeQuery`** for DTO projections (`Object[]`) and read-only reporting. For **writable** work, prefer mutating **managed entities** (dirty checking at flush) over bulk JPQL `UPDATE`/`DELETE` or native `executeUpdate()` on entity-mapped tables unless the PR documents a justified exception. Prefer **named parameters** (`:name`) in native SQL for clarity and safe binding. `*JpaDao` classes are `@Repository` and `@Transactional` where appropriate; `EntityManagerFactory` and `JpaTransactionManager` are wired in `WebConfig` (runtime) and `TestPersistenceConfig` (tests). Entities are scanned from `ar.edu.itba.paw.models`.
- **Entity mapping**: Prefer `@ManyToOne` / associations with `FetchType.LAZY` where the domain already models them (e.g. `Reservation` → `Car`, `Car` → `User` owner). Older scalar-FK + explicit JQL join patterns may still appear in places; do not introduce new ones without cause. Consider `OpenEntityManagerInViewFilter` for lazy navigation inside REST handlers (not view rendering).
- **Enum persistence**: taxonomy enums (`Car.Type`, …) may use `@Enumerated(EnumType.STRING)`; lifecycle enums (`Car.Status`, `Reservation.Status`) use **`AttributeConverter`** implementations that persist **lowercase** names to match legacy SQL and `LOWER(status)` filters.
- **IDs**: sequences use `@GeneratedValue(strategy = GenerationType.SEQUENCE, …)` with `@SequenceGenerator(…, allocationSize = 1)` aligned with PostgreSQL sequences.
- **Pagination**: paginate in SQL (`LIMIT`/`OFFSET` + separate `COUNT`) — never overfetch and slice in memory. For entity queries with joins, avoid relying on `setFirstResult` / `setMaxResults` alone on large sets — prefer ID-page + `IN` fetch or DTO-native queries.
- **Configuration**: Java `@Configuration` (`WebConfig`, `SpringMailConfig`, `WebAuthConfig`, …) plus `web.xml` for servlet/filter bootstrap (Jersey `ServletContainer` as a **filter** on `/api/*`). Properties from `application/application.properties`; profile overrides from `application/application-{profile}.properties` if present. `hibernate.hbm2ddl.auto=update` in **`application.properties`** is **required by the cátedra** — agents must not switch it to `none`/`validate`; schema evolution still ships as Flyway migrations. Not set in `WebConfig` itself.
- **Dependency versions**: Root `pom.xml` `<dependencyManagement>`; child POMs omit versions where managed. JPA stack: Hibernate 5.6.x, `javax.persistence-api` 2.2, `spring-orm` aligned with Spring 5.3. Jersey version: `jersey.version` in root POM.
- **UI**: source in `frontend/src/`; production assets come from `frontend/dist/` (`index.html` + `/public/`) and are merged into the WAR and Jetty composite webapp at build time. Static images not produced by Vite live under `webapp/src/main/webapp/assets/`. Deep links are handled by `SpaFallbackFilter` (HTML GETs → `/index.html`).
- **Component scan** (`WebConfig`): `ar.edu.itba.paw.webapp.controller`, `.exception.mapper`, `.util`, `.support`, `.security`, `.validation`, `.config.properties`, plus `ar.edu.itba.paw.services`, `.persistence`, `.mail`, `.policy`, `.scheduling`, `.util`. **Excludes** Spring `@Controller` (no MVC controllers). Servlet listeners are declared in `WEB-INF/web.xml`.

### Domain overview

A `User` owns `Car`s. A `Car` has price/status and availability (`CarAvailability` / periods). Other users create `Reservation`s. `Image`s are stored as byte arrays and linked via `CarPicture`.

Key enums (on model classes): `Car.Type`, `Car.Powertrain`, `Car.Transmission`, `Car.Status` (active, paused, admin_paused, lack_doc, unavailable, deactivated), `Reservation.Status` (pending, accepted, started, participant/automated cancellation variants, finished — see `Reservation.Status` in code).

## Technologies

- **Backend**: Java 21, Spring 5.3 (Context, TX, ORM, JDBC, Context Support for mail), Spring Security **5.8.10**, **Jersey 2.x** (JAX-RS) for the HTTP API.
- **Database**: PostgreSQL (runtime), HSQLDB (DAO / persistence tests). Baseline **`db/ryden_baseline.sql`** + **Flyway** (`V2__`, `V3__`, … under `classpath:db/migration/`).
- **ORM**: Hibernate / JPA (`EntityManager`, `LocalContainerEntityManagerFactoryBean` in `WebConfig`).
- **API contract**: repo-root **`openapi.yaml`** (vendor MIME types, hypermedia links, JWT headers, `Link` pagination). Consumed by backend and frontend contract tests — not a generated client.
- **Web UI**: React 18 + TypeScript + Vite 5, React Router 6, TanStack Query, Zustand, Bootstrap 5 + react-bootstrap, i18next, Flatpickr.
- **Email**: JavaMail, **Thymeleaf** HTML (`services/src/main/resources/mail/html/`), separate `ResourceBundleMessageSource` for mail copy (`mail/messages/MailMessages` + `_es` / `_en`). SMTP config in `webapp` (`mail/config/emailconfig.properties`, `mail/config/javamail.properties`).
- **Build**: Maven reactor including `frontend`. **Runtime**: Jetty (`jetty-maven-plugin` on the `webapp` module) or Tomcat WAR.

## Database

Credentials live in **`webapp/src/main/resources/application/application.properties`**. **`RydenDataSourceFactory`** builds the PostgreSQL `DataSource`: it runs the idempotent baseline **`classpath:db/ryden_baseline.sql`**, then **Flyway** on **`classpath:db/migration`** (files under `webapp/src/main/resources/db/migration/`).

Tests use **in-memory HSQLDB** via `TestPersistenceConfig` (`schema-hsqldb.sql` in `persistence/src/test/resources`) — no PostgreSQL required for tests. Where useful, tests combine `JdbcTemplate` and the test `EntityManagerFactory` on the same `DataSource` to assert persisted state independently of the DAO under test.

### Flyway migrations

Flyway is configured in **`RydenDataSourceFactory`**: `baselineOnMigrate(true)`, `baselineVersion("1")`, `failOnMissingLocations(true)`.

New migrations: `V<number>__<description>.sql` (e.g. `V45__car_catalog_name_unique.sql`). Recent examples: `V44__reviews_strong_entity_id.sql`, `V2__users_extend_profile_and_auth.sql`, `V3__email_verification_codes.sql`. Do not rename historical Flyway files once applied — checksums must stay stable across deploys.

## Internationalization (i18n)

- **Server messages / errors**: `ReloadableResourceBundleMessageSource` with `classpath:messages/messages` and `classpath:messages/exception/exception-messages`. Used by Jersey exception mappers, Bean Validation, and related server copy. Spanish and English property files.
- **Server locale**: `RydenLocaleResolver` — **does not** use `Accept-Language`. Priority: signed-in user's `latest_locale`, else `RYDEN_LOCALE` cookie, else **Spanish** (`SupportedLocales.DEFAULT`). `LocaleMessages` resolves keys via `MessageSource` + `LocaleContextHolder`.
- **SPA UI**: **i18next** + react-i18next in `frontend/src/i18n/` — default **`es`**, fallback **`en`**. Feature modules export `*I18n` `{ es, en }` (e.g. `features/profile/i18n.ts`) merged with shared `locales/*.json`. Logged-in preference can sync via user `latestLocale` (PATCH `/users/{id}`).
- **Mail**: `mail/messages/MailMessages.properties`, `MailMessages_es.properties`, `MailMessages_en.properties` under **services**. `@Async` mail must not rely on `LocaleContextHolder` — capture locale on the request thread (e.g. reservation mail payload getters).
- **Exception keys**: `exception-messages.properties`, aligned with `ar.edu.itba.paw.exception.MessageKeys`.

## Email

- Configured in `SpringMailConfig` (`mail/config/emailconfig.properties`, `mail/config/javamail.properties` under webapp).
- HTML templates use keys such as `mail.reservationConfirmation.*` (templates under `services/.../mail/html/`).
- Async sending uses `mailTaskExecutor` from `WebConfig`.

## Security

Configured in **`WebAuthConfig`**: Spring Security 5.8.10, `@EnableWebSecurity`, `SecurityFilterChain`, `RydenAuthenticationProvider`, `RydenUserDetailsService`, **`JwtAuthenticationFilter`**. **Stateless** JWT (`SessionCreationPolicy.STATELESS`); **CSRF disabled**; no remember-me / form-login session auth.

- **Login**: `Authorization: Basic` on a probe request → response headers `X-Access-Token` / `X-Refresh-Token`; `Link` rel `authenticated-user` carries the user URN (no `/me`).
- **API calls**: `Authorization: Bearer <access>`; refresh with the refresh token when access expires (SPA retries once on 401).
- **SPA storage**: Zustand session store + localStorage (`frontend/src/session/`).

**Do not** add or replace auth configuration outside `WebAuthConfig`. Do not reintroduce cookie session login for the API.

## Dates and business rules (high level)

- Car availability and reservation datetimes use wall zone `AppTimezone.WALL_ZONE` (Argentina) when parsing server-side. `AppTimezone` (in `models/util/`) is the single source of truth — never hardcode `ZoneId.of("America/Argentina/Buenos_Aires")` again.
- **Publishing / availability**: valid date order; period end not before **today** in that zone (`CarAvailabilityServiceImpl` and related car services).
- **Reserving**: pickup day not before **today**; interval must fit published availability (`ReservationPricingServiceImpl` / reservation services).
- **Flatpickr**: `minDate: 'today'` in SPA calendars (e.g. `useSearchDatePickers`, `DetailReservationInlineCalendar`) complements server validation.

## Development conventions

### Coding style

- **DI**: Constructor `@Autowired` as in existing code.
- **Service ↔ persistence**: Each `*ServiceImpl` injects its own DAOs. Cross-aggregate access goes through peer services (`UserService`, `ReservationService`, …). Use `@Lazy` on one constructor param when two services need each other to break cycles.
- **Scheduling** (`services/.../scheduling` or `ar.edu.itba.paw.scheduling`): `@Scheduled` beans call services only, not DAOs.
- **Validation**: `LocalValidatorFactoryBean` in `WebConfig`, bound into Jersey via `SpringValidatorBinder` / `ValidationContextResolver`. Use `@Valid` and form validation support on resources — not Spring MVC validators.
- **Javadoc**: Public contracts in `*-contracts` in **English**. Avoid HTML `<p>` in Javadoc; use extra `*` lines or `{@code}` / `{@link}`. Resources: short **English** class summary where it helps.

### Quality and security (recurring audit)

- **Spring versions**: Root `pom.xml` (`spring.version`, `spring-security.version`, `jersey.version`).
- **JAX-RS resources**: Call **services** only, not DAOs. No embedded business rules or direct mail sends; use service APIs. Prefer constructor injection and minimal visibility.
- **Exceptions & UX**: Domain failures → `RydenException` + message keys; Jersey **`ExceptionMapper`s** under `webapp.exception.mapper` must not expose raw `Throwable#getMessage()` to clients (i18n keys / structured error DTOs).
- **SQL / queries**: Use parameterized JQL or native SQL with **bound parameters** (named or positional); never concatenate user input into queries.
- **N+1 queries (strictly prohibited)**: Never return a `List<entity>` from a DAO and let callers trigger per-row lazy loads on LAZY associations. DAO methods that return a list of entities must pre-hydrate every association the known caller chain will navigate — typically via `JOIN FETCH` in JPQL, or ID-page + `IN` fetch / DTO-native projections (see **Pagination**). FK-only convenience accessors like `Reservation#getCarId()` / `#getRiderId()` are safe and need no fetch; property navigation across a non-FK association (`car.getOwner()`, `carModel.getBrand()`, …) is not. The fix belongs in the DAO; services and resources must not work around it. Existing pattern examples: `findReservationsWithOverdueRefundProof`, `findReservationsRequiringRefundProofForOwner`, and `loadReservationCardsByIdNativeQuery` in `ReservationJpaDao`.
- **Logging**: Production `logback/logback-prod.xml` (typically **INFO+** for `ar.edu.itba.paw`); local may use `logback-local.xml` with **DEBUG** or **TRACE** on persistence packages when diagnosing car/reservation SQL. Use **SLF4J 2** with parameterized messages — never string concatenation. **DEBUG** must use the fluent API (`LOGGER.atDebug()…log()`), not `LOGGER.debug(…)`; use `.setCause(throwable)` when the catch block swallows an exception but still needs traceability. **INFO** / **WARN** / **ERROR** use the matching fluent helpers (`atInfo`, `atWarn`, `atError`) for the same style.
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
- **DAO integration tests** (`*JpaDaoTest`, `DaoIntegrationTestSupport`): After a **write** (`create*`, `update*`, `delete*`), call **`em.flush()`** when using JPA in the same transaction, then assert with **`JdbcTemplate`** / SQL fixtures — **never** verify persistence only via another method on the **same** DAO (e.g. `createCar` + `getCarById`) and avoid asserting writes solely with **`em.find`** (first-level cache). For ordered reads with fixtures, optional ground-truth `ORDER BY` via `JdbcTemplate`.
- **No interaction verification**: No `verify`, `verifyNoInteractions`, `never()`, `times()`, `InOrder`, captors for call wiring.
- **Do not emulate verify**: No counters or collector lists whose sole purpose is call counts.
- **Matchers**: Do not use `Mockito.anyLong()` (or `any*()`) as the **value** inside `when(...)` — use fixed literals.
- **Strict Mockito**: Remove unused stubbings (`UnnecessaryStubbingException`).

#### Frontend (`frontend/`, Vitest)

- **Primary style — contract tests** in `*.contract.test.ts`: alignment with `openapi.yaml`, hypermedia (`Link`, `X-Total-Count`, URN resolution under `/webapp/api`), JWT, multipart, and i18n `es`/`en` parity. Prefer these over UI/routing/client-validation unit tests.
- **Unit helpers**: no real network; mock `fetch` when needed (e.g. `dateFormat.test.ts`, `navActive.test.ts`).
- **AAA**: each `it` with `// 1.Arrange`, `// 2.Act`, `// 3.Assert` (or `// 2.Act / 3.Assert` when applicable).
- **Names**: prefix `test` + behaviour (`testParseLinkHeaderReadsNextAndLastRels`).

### Frontend SPA conventions

- **Stack**: React 18, TypeScript, Vite 5, React Router 6 (`basename` from `appBasePath()`), TanStack Query, Zustand session, Bootstrap / react-bootstrap, i18next, Flatpickr.
- **Layout** (`frontend/src/`):
  - `api/` — `HypermediaClient`, JWT helpers, vendor MIME types, URI/URN resolution, contract tests
  - `features/` — domain slices (`admin`, `auth`, `browse`, `owner`, `profile`, `reservations`)
  - `components/` — shell (`Layout`, `NavBar`, `RequireAuth`) + shared `ryden/*` UI kit
  - `router/`, `routes/` — `AppRouter`, `paths.ts`, nav helpers
  - `session/` — Zustand store + session client
  - `i18n/`, `styles/` — locales + `ryden-theme.css` / component CSS
- **Hypermedia**: navigate via DTO `links` and `HypermediaClient` (`follow()`, `followLinkCollection`, `getLinkCollectionPage` in **`frontend/src/api/client.ts`**). Never invent API URLs from bare IDs.
- **API discovery**: on boot, **`frontend/src/api/apiDiscovery.ts`** loads `GET /api/` (`ApiIndex`) and caches top-level `links` (including `config` for **`clientConfig.ts`**). Feature code should follow those links rather than hardcoding `/webapp/api/...` paths.
- **MIME types**: always versioned vendor types from `mediaTypes.ts` (`application/vnd.paw.*.v1+json`). Never use bare `application/json` for API resources.
- **Feature modules**: pages, hooks, `api.ts`, `types.ts`, routes, and `i18n.ts` live with the feature; shared interaction UI under `components/ryden/`.
- **SPA routes**: English path shapes in `routes/paths.ts` (aligned with legacy URL shapes for mail CTAs and bookmarks).

### Message keys

Domain / validation exception copy: **`exception-messages.properties`** (+ `_es`), consistent with **`MessageKeys`**.

### Application properties

- **Main**: `webapp/src/main/resources/application/application.properties` — port, context path (`/webapp` local; Pampero `…/paw-2026a-08`), uploads, validation, pagination, reservation timing, JWT, `app.scheduler.*` crons/zones. Keep `hibernate.hbm2ddl.auto=update` (cátedra); do not “harden” it to `none`.
- **Profiles**: `application/application-local.properties`, `application/application-deployed.properties` (examples in folder); secrets not committed.
- **Mail**: `mail/config/emailconfig.properties`, `mail/config/javamail.properties` under `webapp/src/main/resources/mail/`.

### Directory structure (per module)

- `src/main/java`, `src/main/resources`, `src/test/java` as usual.
- `webapp/src/main/webapp`: `WEB-INF/web.xml`, `assets/` (static images). Vite output (`index.html`, `public/*`) is **not** committed under `webapp/src/main/webapp` — it is copied from `frontend/dist/` into the WAR / Jetty composite at build time.
- `frontend/src`: see **Frontend SPA conventions** above.

### Key services and DAOs (orientation)

- **User**: `UserService` / `UserDao`.
- **Car & availability**: `CarService` / `CarDao`, `CarAvailabilityService` / `CarAvailabilityDao`.
- **Reservation**: `ReservationService` (and related workflow/pricing services) / `ReservationDao`.
- **Email & verification**: `EmailService`, `EmailVerificationService`, `PasswordResetService` and related DAOs.
- **Images**: `ImageService` / `ImageDao`, `CarPictureService` / `CarPictureDao`.
- **Reviews / favorites / location**: matching `*Service` / `*Dao` pairs under the same feature packages.

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

## API / resource cross-cutting concerns

### REST URL routing

- **Resource identity on GET**: Identifiers that name the resource being accessed (`carId`, `userId`, `reservationId`, `documentType`, …) belong in the path as JAX-RS `@PathParam`s — e.g. `GET /cars/{carId}`, `GET /users/{userId}/profile`, `GET /users/{userId}/documents/{documentType}` — not as `@QueryParam`s. Prefer `/cars/123` over `/car-detail?carId=123`.
- **Query params for everything else**: Pagination, sorting, filters, and UI state (`page`, `sort`, `status`, `role`, `src`, `fromCar`, `tab`, …) stay as query params.
- **Consistency**: New resources, OpenAPI paths, SPA routes (`frontend/src/routes/paths.ts`), mail CTA URLs, and client `follow()` targets must agree. Canonical API base (local): `/webapp/api`. Pampero: `/paw-2026a-08/api`. Canonical SPA examples: owner car detail and reservation detail paths in `paths.ts`.
- **Contract source of truth**: [openapi.yaml](openapi.yaml) at the repo root — agents should consult it for MIME types, link relations, auth headers, and collection shapes before inventing endpoints.

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

Frontend helpers: `followLinkCollection`, `getLinkCollectionPage` in `frontend/src/api/client.ts`.

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

The SPA page body uses Bootstrap's `bg-light` (`#f8f9fa`, "cream/off-white"). Any section or card rendered directly on this background **must** carry `bg-white` so it visually pops. Nested elements placed *inside* a `bg-white` section that need their own background should use `bg-light` (the cream tone) to keep the alternating contrast going.

Quick checklist when adding a new section card:
- Section sits on `bg-light` page → add `bg-white` to the card/container.
- Sub-element sits on `bg-white` card → use `bg-light` for its background.

This applies to React components and CSS under `frontend/` (especially `styles/ryden-theme.css` and feature styles).
