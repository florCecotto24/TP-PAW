# PAW — Ryden

Car rental platform. **Java 21**, **stateless REST** backend (Jersey + Spring) and **SPA** frontend (React + Vite + TypeScript) packaged in a multi-module Maven WAR.

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **PostgreSQL** running **before** you start the app (schema / Flyway use the datasource from your active profile). Example check:

  ```bash
  psql -h localhost -U pawdbuser -d paw
  ```

- Adjust host, user, and database to match `application-local.properties` in `webapp/src/main/resources/` (or your own overrides).

Node.js is **not required** on the build machine: the `frontend` module downloads Node v20 via `frontend-maven-plugin`. For isolated SPA development (`npm run dev`), having Node 20+ locally is recommended.

### Environment properties (required)

Startup **requires** **`.properties`** files that are **not** in the repository (they are `.gitignore`d). **Create them from the matching `.example`** files in the same directory:

| Profile / use case | File to create | Template |
|--------------------|-----------------|----------|
| Local development (`-Dspring.profiles.active=local`, e.g. `mvn jetty:run` on the webapp module) | `webapp/src/main/resources/application/application-local.properties` | `application-local.properties.example` |
| Deployment (without the `local` profile, e.g. Tomcat on Pampero) | `webapp/src/main/resources/application/application-deployed.properties` | `application-deployed.properties.example` |

Those files hold JDBC settings, SMTP password, JWT secret (recommended in deployed), and `mail.app.base.url`, in addition to shared settings in committed `application.properties`. Without the `.properties` file for the active profile, the Spring context will not start.

## Build and run (from repository root)

```bash
mvn clean install
```

Or, for a quicker local backend + SPA on one port:

```bash
mvn compile -pl webapp -am
mvn jetty:run -pl webapp
```

`mvn compile` / `mvn package` build the frontend (`npm install` + `npm run build` in the `frontend` module) and package `frontend/dist/` into the WAR. The SPA **Vite `base` is always `/paw-2026a-08/`** (same WAR for Pampero and local Tomcat/Jetty). **Jetty** serves a merged tree at `webapp/target/webapp-composite/`; run `compile -pl webapp -am` before `jetty:run` so hashed bundles match `index.html`. Hashed Vite assets are **not** stored under `webapp/src/main/webapp/public/` in git.

**JVM options** (Spring profile + test Logback), same as configuring **VM options** in your IDE when you run Jetty/Tomcat:

```text
-Dspring.profiles.active=local
-Dlogback.configurationFile=classpath:logback/logback-local.xml
-Dfile.encoding=UTF-8
```

From a Unix shell at the repo root you can use:

```bash
export MAVEN_OPTS="-Dspring.profiles.active=local -Dlogback.configurationFile=classpath:logback/logback-local.xml -Dfile.encoding=UTF-8"
mvn compile -pl webapp -am
mvn jetty:run -pl webapp
```

Alternatively, deploy the **`webapp`** WAR to **Tomcat** (copy as `paw-2026a-08.war`). Public URL on Pampero: `http://pawserver.it.itba.edu.ar/paw-2026a-08/`. Plain `mvn clean package` already embeds that SPA base

```bash
mvn clean package -pl webapp -am
# artefacto: webapp/target/webapp.war → copiar como paw-2026a-08.war
```

Local Jetty/Tomcat use the **same** context path **`/paw-2026a-08`**. Open `http://localhost:8080/paw-2026a-08/`. The SPA and API share that origin (`/paw-2026a-08/api/…`). For SPA-only iteration, `cd frontend && npm run dev` (Vite `base: '/'`, proxies API to Jetty).

### SPA dev server (optional)

With the backend on `:8080`:

```bash
cd frontend && npm install && npm run dev
```

Vite proxies REST resources to the backend (see `frontend/vite.config.ts`).

## Demo account

For demos or manual testing you can sign in with this user. The same credentials work for the associated Gmail inbox (to read e-mail sent by the app during a demo):

- **Email:** `user.ryden.paw@gmail.com`
- **Password:** `ryden.password`

## Tests

```bash
mvn test
```

(`mvn clean install` already runs tests unless skipped.)

- Backend: JUnit 5 + Mockito (`services`), HSQLDB (`persistence`)
- Frontend: `mvn test -pl frontend` or `cd frontend && npm test` (Vitest)

## Deployed log files (HTTP)

On **Pampero**, set **`-DLOG_DIR=...`** to the filesystem directory that HTTP serves as **`/logs/`** (e.g. on `pawserver.it.itba.edu.ar`). Logs roll **daily** by level (same basename for app and platform). Override with **`-DDEPLOY_LOG_BASENAME=...`** if needed (default `paw-2026a-08-webapp`).

| Log | URL pattern (replace `yyyy-MM-dd` with the date) |
|-----|--------------------------------------------------|
| **INFO only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp-info.yyyy-MM-dd.log` |
| **WARN only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp-warn.yyyy-MM-dd.log` |
| **ERROR only** (`ar.edu.itba.paw`) | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp-error.yyyy-MM-dd.log` |
| **WARN only** (frameworks / libraries, root) | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp-platform-warn.yyyy-MM-dd.log` |
| **ERROR only** (frameworks / libraries, root) | `http://pawserver.it.itba.edu.ar/logs/paw-2026a-08-webapp-platform-error.yyyy-MM-dd.log` |

Appenders are defined in `webapp/src/main/resources/logback/logback-includes.xml`. **`LOG_BASENAME`** remains for any future use; platform files now use **`DEPLOY_LOG_BASENAME`** like the app split files.

Base Java package: `ar.edu.itba.paw`.
