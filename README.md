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

### Environment properties (required)

Startup **requires** **`.properties`** files that are **not** in the repository (they are `.gitignore`d). **Create them from the matching `.example`** files in the same directory:

| Profile / use case | File to create | Template |
|--------------------|-----------------|----------|
| Local development (`-Dspring.profiles.active=local`, e.g. `mvn jetty:run` on the webapp module) | `webapp/src/main/resources/application/application-local.properties` | `application-local.properties.example` |
| Deployment (without the `local` profile, e.g. Tomcat on Pampero) | `webapp/src/main/resources/application/application-deployed.properties` | `application-deployed.properties.example` |

Those files hold JDBC settings, SMTP password, *remember-me* key, and `mail.app.base.url`, in addition to shared settings in committed `application.properties`. Without the `.properties` file for the active profile, the Spring context will not start.

## Build and run (from repository root)

```bash
mvn clean install
mvn jetty:run -pl webapp
```

**JVM options** (Spring profile + test Logback), same as configuring **VM options** in your IDE when you run Jetty/Tomcat:

```text
-Dspring.profiles.active=local
-Dlogback.configurationFile=classpath:logback/logback-local.xml
-Dfile.encoding=UTF-8
```

From a Unix shell at the repo root you can use:

```bash
export MAVEN_OPTS="-Dspring.profiles.active=local -Dlogback.configurationFile=classpath:logback/logback-local.xml -Dfile.encoding=UTF-8"
mvn jetty:run -pl webapp
```

Alternatively, deploy the **`webapp`** WAR (`webapp/target/webapp.war`) to **Tomcat** or another servlet container with the same JVM options, datasource, and profile settings.

Jetty is configured on port **8080** in the parent POM. The app may use a servlet context path (see `server.servlet.context-path` in `application.properties`); try `http://localhost:8080/` and `http://localhost:8080/webapp/` if the home page is not at the root you expect.

## Demo account

For demos or manual testing you can sign in with this user. The same credentials work for the associated Gmail inbox (to read e-mail sent by the app during a demo):

- **Email:** `user.ryden.paw@gmail.com`
- **Password:** `ryden.password`

## Tests

```bash
mvn test
```

(`mvn clean install` already runs tests unless skipped.)

## Deployed log files (HTTP)

On **Pampero**, the process log directory is exposed under **`/logs/`** on the host (e.g. `pawserver.it.itba.edu.ar`). File names use **defaults** from Logback; override with JVM **`-DLOG_BASENAME=...`** (rolling files) and **`-DDEPLOY_LOG_BASENAME=...`** (daily file) if you change them in deployment.

| Log | URL pattern (replace `yyyy-MM-dd` with the date) |
|-----|--------------------------------------------------|
| **General (daily, all application levels written to the rolling daily appender)** | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp.yyyy-MM-dd.log` |
| **INFO only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-app-info.log` |
| **WARN only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-app-warn.log` |
| **ERROR only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-app-error.log` |

The **general** daily file is the same one referenced in `webapp/src/main/resources/logback.xml`. The level-specific files are defined in `webapp/src/main/resources/logback/logback-includes.xml`. Framework and library **WARN** / **ERROR** lines (not under `ar.edu.itba.paw`) go to `paw-platform-warn.log` and `paw-platform-error.log` in the same `/logs/` directory.

Base Java package: `ar.edu.itba.paw`.
