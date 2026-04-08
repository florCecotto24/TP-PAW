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

The app is served at `http://localhost:8080/` (context path is `/api` per `application.properties`, so routes like `/api/home`).

## Database

Database credentials are in `webapp/src/main/resources/application.properties`. The schema is auto-applied on startup from `persistence/src/main/resources/schema.sql` via `DataSourceInitializer` in `WebConfig.java`. The schema uses `CREATE TABLE IF NOT EXISTS`, so it is safe to re-run.

Tests use an **in-memory HSQLDB** database configured in each module's `TestConfiguration.java` — no PostgreSQL required for testing.

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
- **Component scan**: `WebConfig` scans `ar.edu.itba.paw.webapp.controller`, `ar.edu.itba.paw.services`, and `ar.edu.itba.paw.persistence`.

### Domain overview

A `User` owns `Car`s. A `Car` can have a `Listing` (rental offer) with a price and availability periods (`listing_availability`). Other users create `Reservation`s against a listing. `Image`s are stored as byte arrays in the DB and associated to cars via `CarPicture`.

Key enums (defined inside model classes): `Car.Type`, `Car.Powertrain`, `Car.Transmission`, `Listing.Status` (active/paused/finished), `Reservation.Status` (accepted/started/cancelled/finished).
