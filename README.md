# PAW — Ryden

Multi-module **Spring MVC** + **JSP** web application. **Java 21**.

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **PostgreSQL** running **before** you start the app (schema / Flyway use the datasource from your active profile). Example check:

  ```bash
  psql -h localhost -U pawdbuser -d paw
  ```

- Adjust host, user, and database to match `application-local.properties` in `webapp/src/main/resources/` (or your own overrides).

## Build and run (from repository root)

```bash
mvn clean install
mvn jetty:run -pl webapp
```

**JVM options** (Spring profile + test Logback), same as configuring **VM options** in your IDE when you run Jetty/Tomcat:

```text
-Dspring.profiles.active=local
-Dlogback.configurationFile=<absolute-path-to-repo>/webapp/src/test/resources/logback-test.xml
```

From a Unix shell at the repo root you can use:

```bash
export MAVEN_OPTS="-Dspring.profiles.active=local -Dlogback.configurationFile=$(pwd)/webapp/src/test/resources/logback-test.xml"
mvn jetty:run -pl webapp
```

Alternatively, deploy the **`webapp`** WAR (`webapp/target/webapp.war`) to **Tomcat** or another servlet container with the same JVM options, datasource, and profile settings.

Jetty is configured on port **8080** in the parent POM. The app may use a servlet context path (see `server.servlet.context-path` in `application.properties`); try `http://localhost:8080/` and `http://localhost:8080/api/` if the home page is not at the root you expect.

## Tests

```bash
mvn test
```

(`mvn clean install` already runs tests unless skipped.)

## Maven modules

| Module | Purpose |
|--------|---------|
| **common** | Small shared helpers under `ar.edu.itba.paw.common`. |
| **models** | Domain entities and DTOs (`models.domain`, `models.dto`, `models.util`). |
| **persistence-contracts** | DAO interfaces (`persistence`) used by services and JDBC. |
| **persistence** | JDBC implementations (`persistence.jdbc`), catalogs (`persistence.catalog`), SQL (`schema.sql`). |
| **service-contracts** | Service interfaces (`services`), exceptions, message keys (`exception`). |
| **services** | Business logic (`services`, `services.policy`), DAO integration, application rules. |
| **webapp** | WAR: Spring MVC (`webapp.controller`), configuration (`webapp.config`), security (`webapp.security`), form validation (`webapp.validation`), forms (`webapp.form`), Flyway migrations (`webapp/src/main/resources/db/migration/`), JSPs and static assets. |

Base Java package: `ar.edu.itba.paw`.
