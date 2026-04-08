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

- **Backend**: Java 21, Spring 5.3 (MVC, JDBC, Context, TX, Context Support for mail).
- **Database**: PostgreSQL (runtime), HSQLDB (DAO / persistence tests).
- **Web UI**: JSP (`webapp/src/main/webapp/WEB-INF/views/`), Spring form tags, custom JSP tags under `WEB-INF/tags/`.
- **Client scripts**: Shared JS in `webapp/src/main/webapp/js/` (e.g. **Flatpickr** for date/range pickers, loaded from CDN in `header.jsp` / `footer.jsp`).
- **Email**: JavaMail, **Thymeleaf** HTML templates (`webapp/src/main/resources/mail/html/`), separate `ResourceBundleMessageSource` for mail copy (`mail/MailMessages` + locale variants).
- **Build**: Maven.
- **Runtime server**: Jetty (`jetty-maven-plugin` on the `webapp` module).

## Building and Running

### Prerequisites

- Java 21
- Maven
- PostgreSQL reachable with credentials defined in **`webapp/src/main/resources/application.properties`** (`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`). Optional overrides: `application-${spring.profiles.active}.properties` (see `@PropertySources` in `WebConfig`).

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
- **Configuration**: Java `@Configuration` (`WebConfig`, `SpringMailConfig`) plus `web.xml` for servlet bootstrap.
- **Persistence**: Plain SQL with `JdbcTemplate`; schema applied via `DataSourceInitializer` and `schema.sql` (under `persistence` resources, on the classpath for the webapp module).

### Dependency management

- **Centralized versions**: Maven properties and `<dependencyManagement>` in the root `pom.xml`.
- **Module POMs**: External dependencies without repeating versions where managed by the parent.
- **Internal modules**: Use `${project.version}` for sibling artifacts.

### Testing

- **Unit tests**: JUnit 5 and Mockito (`services`, `models`).
- **Persistence tests**: HSQLDB (`TestPersistenceConfig`, DAO integration-style tests under `persistence/src/test`).
- **Service tests**: Mock DAOs and collaborators; avoid `Mockito.anyLong()` (and similar) as **assigned values** — use fixed literals inside `when(...)` / `verify(...)`.
- **Strict Mockito**: Remove unused stubbings or they fail with `UnnecessaryStubbingException`.

### Message keys

- Domain / validation strings for exceptions: **`exception-messages.properties`** (and `_es`), keyed consistently with **`ar.edu.itba.paw.exception.MessageKeys`**.

### Directory structure (per module)

- `src/main/java`: Java sources.
- `src/main/resources`: Config, SQL, i18n bundles, mail templates.
- `src/test/java`: Tests.
- `webapp/src/main/webapp`: JSPs, CSS, JS, `WEB-INF/web.xml`.
