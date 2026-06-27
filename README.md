# PAW — Ryden

Plataforma de alquiler de autos. **Java 21**, backend **REST stateless** (Jersey + Spring) y frontend **SPA** (React + Vite + TypeScript) empaquetados en un WAR multi-módulo Maven.

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **PostgreSQL** running **before** you start the app (schema / Flyway use the datasource from your active profile). Example check:

  ```bash
  psql -h localhost -U pawdbuser -d paw
  ```

- Adjust host, user, and database to match `application-local.properties` in `webapp/src/main/resources/` (or your own overrides).

Node.js **no es obligatorio** en la máquina de build: el módulo `frontend` descarga Node v20 vía `frontend-maven-plugin`. Para desarrollo SPA aislado (`npm run dev`) sí conviene tener Node 20+ local.

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
mvn jetty:run -pl webapp
```

`mvn compile` / `mvn package` compilan también el frontend (`npm install` + `npm run build` en el módulo `frontend`) y empaquetan `frontend/dist/` en el WAR.

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

Alternatively, deploy the **`webapp`** WAR (`webapp/target/webapp.war`) to **Tomcat** on Pampero. Para Tomcat el build del frontend usa context path `/webapp/` (`npm run build:tomcat` vía Maven).

Jetty local usa puerto **8080** y context path **`/`** (`server.servlet.context-path` en `application.properties`). La SPA y la API comparten el mismo origen; rutas de la API son sustantivos REST (`/users`, `/cars`, …) sin prefijo `/api`.

### SPA dev server (opcional)

Con el backend en `:8080`:

```bash
cd frontend && npm install && npm run dev
```

Vite proxea los recursos REST al backend (ver `frontend/vite.config.ts`).

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
- Frontend: `mvn test -pl frontend` o `cd frontend && npm test` (Vitest)

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
