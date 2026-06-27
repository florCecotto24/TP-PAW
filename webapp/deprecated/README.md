# Código MVC/JSP legacy (no compilado ni empaquetado)

Este árbol conserva controllers Spring MVC, JSP, tags y utilidades de soporte **solo como referencia histórica**.

- **Java:** `deprecated/main/java/` — excluido del `maven-compiler-plugin` (fuera de `src/main/java`).
- **Vistas:** `src/main/webapp/deprecated/` — excluido del WAR via `packagingExcludes`.
- **Tests:** `deprecated/test/java/` — no compilados; Surefire excluye `**/deprecated/**` en `src/test`.

El runtime activo es **Jersey + SPA** (`frontend/dist` en el WAR).
