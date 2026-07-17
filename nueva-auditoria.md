# Nueva auditoría profunda — TP-PAW (Ryden)

> **Fecha:** 2026-07-16 (tarde). **Alcance:** proyecto completo (models → persistence → services → webapp → frontend), contrastado con `context/FINAL_PAW_V1/{LINEAMIENTOS-SPA-REST,BIBLIA_REST_CATEDRA,CONSULTAS-CATEDRA}.md`, correcciones de otros TPs (`context/correcciones-*`, `correciones-dic-2025.md`), **MenuMate** (`context/menumate-master`, nota 9) y re-verificación de `context/auditoria-16-jul`.
>
> **Método:** revisión estática exhaustiva + agentes especializados (REST, Security, SPA, Effective Java, funcionamiento, corpus de correcciones) + verificación manual de hallazgos de riesgo. Severidades: 🔴 crítica · 🟠 alta · 🟡 media · ⚪ baja.
>
> Convención: rutas relativas a la raíz del repo `TP-PAW/`.

---

## 0. Resumen ejecutivo

Ryden está **claramente por encima del promedio de un TP PAW** y **más alineado con la doctrina de la cátedra que MenuMate en varios ejes** (MIME que no define operación, un solo `PATCH` de usuario, discovery hypermedia, contract tests contra `openapi.yaml`, sin `/me`). El ciclo de alquiler (auto → disponibilidad → reserva → comprobantes → reembolso → review → admin) está completo y defendible.

Desde la auditoría de la mañana (`context/auditoria-16-jul`) se **cerraron los 4 P0 de “bocha directa”** y varios graves. Lo que **sigue abierto** y más puede doler en el final / oral:

1. **OTP de reset = sesión usable** (access corto + refresh con roles de negocio) — 🟠
2. **SPA reconstruye URNs API desde IDs de ruta** en detalle/bookmarks — 🟠 (error típico de devoluciones)
3. **Bytes de galería sin `requireViewableCar`** — 🟡/🟠
4. **`BookableBrowseSupport` pagina en memoria** el browse sin fechas — 🟠
5. **Zonas grises REST** a preparar para oral (Accept dual en representación de `GET /cars`, `DELETE` colección availabilities no documentado, plate por rol dentro del mismo MIME)

**Controllers:** ningún DAO / `EntityManager` / `new Car|User|…` — capa web limpia en el sentido que la cátedra marca como “error conceptual grave” en otros grupos.

---

## 1. Re-verificación vs `auditoria-16-jul` (mañana)

| Ítem (16-jul) | Estado ahora | Evidencia |
|---|---|---|
| 1.1 JSESSIONID / `jsp-file` | ✅ **Cerrado** | `SessionTrackingDisablingListener` en `web.xml:33-35`; mapping default servlet sin `jsp-file` (`web.xml:122-130`) |
| 1.2 Refresh no autentica | ✅ **Cerrado** | `JwtAuthenticationFilter.handleBearer` aplica SecurityContext + rota tokens (`:140-157`); SPA reintenta con access absorbido |
| 1.3 POST brands/models 201 si existe | ✅ **Cerrado** | `CarBrandServiceImpl.createUnvalidated` → `CarBrandConflictException` (`:77-79`) |
| 1.4 Cache-Control `/public` | ✅ **Cerrado** | `SpaFallbackFilter` hace pass-through (`:31-34`); `UnconditionalCacheFilter` + dispatcher `FORWARD` (`web.xml:52-57`) |
| 1.5 Claves i18n rider | ✅ **Cerrado** | `exception-messages*.properties` tienen `reservation.rider.listingNotFound` / `cannotReserveOwnListing` |
| 1.6 `links.documents` colgante | ✅ **Cerrado** | sin `documents` en `UserLinks` / `UserDto` |
| 1.7 Flags KYC en user público | ✅ **Cerrado** | sin `licenseValidated` etc. en `UserDto` |
| Dual Accept decide membresía de `GET /cars` | ✅ **Mitigado** | admin catalog vía `?status=all` (`CarController:168-170`, `:183-187`); Accept elige representación summary vs full |
| Baja por PATCH `deactivated` | ✅ **Cerrado** | `applyStatusTransition` rechaza `DEACTIVATED` (`CarServiceImpl:282`) |
| PUT `/cars/{id}` parcial | ✅ **Cerrado** | no hay `@PUT` en `CarController` |
| `GET /neighborhoods/{id}` | ✅ **Cerrado** | `NeighborhoodController:52-58` |
| `EntityEquality` / `Image.toString` | ✅ **Cerrado** | getters + hash por clase; `dataLength` |
| `CarDto.fromCarCard` fabrica enums | ✅ **Cerrado** | campos detail = `null` (`CarDto:89-93`) |
| Uploads sin tope | ✅ **Cerrado** | `BinaryPayloadSupport.readBounded` → 413 |
| Sweeps envueltos en TX facade | ✅ **Cerrado** | `ReservationServiceImpl` delega sweeps **sin** `@Transactional` |
| Refund GET sin headers sensibles | ✅ **Cerrado** | `CacheableBinaryResponses.sensitive` |
| `RydenLocaleResolver` desconectado | ✅ **Cerrado** | bean + `RydenLocaleFilter` en `web.xml` |
| Doble booking sin lock | ✅ **Cerrado** | `lockForReservationWrite` antes del overlap (`ReservationWorkflowServiceImpl:220-222`) |

---

## 2. Funcionamiento

### 2.1 Qué está sólido

- **Dominio completo:** User → Cars → Availability → Reservation (PENDING→ACCEPTED→STARTED→FINISHED / cancelaciones) → payment/refund proofs → reviews → favorites → admin (usuarios, catálogo, pause).
- **Schedulers** llaman services (no DAOs); claim-then-mail con `REQUIRES_NEW`; locale en payloads (no `LocaleContextHolder` en `@Async`).
- **Anti N+1** en caminos calientes (cards, favoritos, mensajes, jobs) con `JOIN FETCH` / ID-page / nativo.
- **Timezone** centralizado en `AppTimezone.WALL_ZONE`.
- **Overlap de reservas** serializado con lock pesimista del auto.

### 2.2 Hallazgos vigentes

| Sev | Hallazgo | Refs |
|-----|----------|------|
| 🟠 | **Browse sin fechas pagina en memoria:** `BookableBrowseSupport` descarga páginas SQL, filtra bookable en JVM y hace `subList`. Escala mal y puede mentir totales bajo carga. | `services/.../car/BookableBrowseSupport.java:23-76` |
| 🟡 | Disponibilidad “effective” / calendarios: `slicePage` en memoria | `CarAvailabilityServiceImpl` |
| 🟡 | Analytics owner (`findCarFinishedReservations` + sum en stream) | `ReservationLifecycleSchedulerServiceImpl` |
| 🟡 | `hibernate.hbm2ddl.auto=update` en `application.properties` + Flyway (dos autoridades de esquema); deployed example = `none` | `application.properties` |
| 🟡 | Chat / pricing: coste alto en caminos largos (días × queries) — vigilar | `ReservationPricingServiceImpl`, mensajes |
| ⚪ | Mes “this month” owner en UTC vs wall AR | lifecycle analytics |
| ⚪ | Bulk `DELETE` JPQL en OTP / reservation_availability (aceptable con justificación) | `*CodeJpaDao`, `ReservationAvailabilityJpaDao` |

### 2.3 Cobertura de tests (funcional)

- Fortaleza: DAO integration HSQLDB, contract OpenAPI (backend + frontend), `PreAuthorizeMethodSecurityTest`, naming `test*` / 0 `verify`.
- Hueco 🟠: **no hay `ReservationPaymentServiceImplTest`** (path money-critical).
- Hueco 🟡: búsqueda flexible Postgres (`generate_series` / `LATERAL`) difícil de cubrir en HSQLDB.

---

## 3. Effective Java / estilo Java

### 3.1 Cumple bien

| Criterio | Estado |
|----------|--------|
| ≤1 constructor público (regla AGENTS) | Casi total; excepción `OtpAttemptLimiter` |
| Constructor injection `@Autowired` | Sí; 0 field injection en main |
| Fluent SLF4J (`atDebug`…) | 0 `LOGGER.debug(` clásicos |
| Tests `test*` + sin `Mockito.verify` | Cumple |
| Lifecycle enums + `AttributeConverter` | `Car.Status`, `Reservation.Status`, … |
| Controllers sin DAOs | Cumple |
| `equals`/`hashCode` JPA-friendly | `EntityEquality` con getters + hash estable por clase |
| `Image.toString` | `dataLength`, no dump del blob |

### 3.2 Hallazgos

| Sev | Hallazgo | Refs |
|-----|----------|------|
| 🟡 | **`OtpAttemptLimiter`**: 2 constructores públicos (prod + test) | `services/.../OtpAttemptLimiter.java` |
| 🟡 | DTOs REST mayormente mutables (setters) — práctico para JAX-RS, lejos de inmutabilidad Ideal | `webapp/.../dto/rest/*` |
| 🟡 | Auto-invocación `this.foo()` frecuente: con `REQUIRED` suele estar OK; olor si mañana se pide `REQUIRES_NEW` en el callee | varios `*ServiceImpl` |
| 🟡 | Bulk `DELETE` OTP / coverage vs dirty-checking por defecto | DAOs citados arriba |
| ⚪ | Checklist AGENTS “todo público con `@Transactional`”: jobs de sweep y mail `@Async` omiten a propósito (documentado). **`AdminServiceImpl` tiene `@Transactional` de clase** — las escrituras admin **sí** están envueltas. |
| ⚪ | `CarModelJpaDao.findById` sin `JOIN FETCH brand` (OEIV lo salva en HTTP) | admin catalog |

---

## 4. Purismo REST / hypermedia

### 4.1 Alineación fuerte con LINEAMIENTOS / BIBLIA

- Recursos sustantivos plurales; sin `/login` / `/me` en API.
- Vendor MIME versionados `application/vnd.paw.*.v1+json`.
- Paginación por `Link` + `X-Total-Count` (no metadata en body).
- Raíz HATEOAS `GET /api/` + discovery en SPA.
- Colecciones: patrón A link-only (similares, favoritos, reviews, reservations) documentado; patrón B embedded donde aplica (browse, pictures, messages, catálogos).
- Status codes 201+Location / 204 / 401 vs 403 razonables.
- Reviews: creación por **URN** `reservationUri` en multipart (no query) — `ReviewsController:91-103`.
- Brands: **409** si ya existe.
- Baja de auto por **DELETE**; pause/activate por **PATCH status**.

### 4.2 Hallazgos REST vigentes

| Sev | Hallazgo | Por qué importa (cátedra / devoluciones) | Refs |
|-----|----------|------------------------------------------|------|
| 🟠 | Cliente inventa `/cars|users|reservations/{id}` desde params de router | “API solo usable si el cliente conoce URNs”; hardcodeo = señal de API incompleta o cliente impuro | ver §6 |
| 🟡 | `DELETE /cars/{id}/availabilities?from&until` (colección) **no está en `openapi.yaml`** | drift contrato; oral | `CarAvailabilityController:187-196` |
| 🟡 | Misma URN `car.v1` con/sin `plate` según viewer | BIBLIA §8 prefiere MIME distintos (como hicieron con user público/privado) | `CarController` get/patch |
| 🟡 | Sort documentado de reservations (`recent`, `start_date`, …) vs sanitizer legacy `campo,dir` | contrato vs realidad | openapi + `ReservationController` |
| 🟡 | `CarController` (~617 líneas): routing rico de listados — orquestación HTTP, no reglas de dominio, pero pesado para oral | fat resource |
| ⚪ | PUT binarios (receipts, profile picture, insurance, documents) — defendible como reemplazo de sub-recurso | — |
| ⚪ | `POST /users` con Content-Type distinto para admin-create — MIME elige *forma* del body; vigilar defensa (no MIME-como-verbo al estilo MenuMate `…activate.v1`) | `UserController` |

### 4.3 Checklist “errores que te bochan” (estado)

| Error típico | ¿Ryden? |
|--------------|---------|
| JSESSIONID en API stateless | No |
| Cache busting sin cache no-condicional | No (`/public` immutable) |
| Links vacíos / URLs inválidas (`…/null`) | No (documents/neighborhoods fijos) |
| Embeber agregado ajeno (`user.cars[]`) | No (links) |
| `/me` | No |
| MIME define la *operación* (activate/password MIME separados como verbo) | Evitado en lo esencial; un `PATCH` user |
| Refresh solo en roundtrip dedicado | No: retry sobre el mismo request |
| Controllers con lógica de negocio / new Entity | No DAOs / no `new` entidades |

---

## 5. Spring Security

### 5.1 Fortalezas

- Stateless JWT: `SessionCreationPolicy.STATELESS`, sin form-login/remember-me; CSRF off coherente (auth por header).
- Basic → `X-Access-Token` / `X-Refresh-Token` + `Link rel="authenticated-user"` (no `Authorization` de respuesta).
- Refresh **autentica** + recarga roles desde DB + rota headers.
- `@EnableMethodSecurity(proxyTargetClass=true)` + beans `*ResourceAccess` + test que protege la decisión.
- Secret JWT fail-fast fuera de local/test; HS256 derivado SHA-256.
- Anti-enumeración credentials; rate-limit OTP; CORS solo perfil `local`.
- Headers CSP / nosniff / frame-deny en API; SpaFallback también setea headers en HTML.

### 5.2 Hallazgos

| Sev | Hallazgo | Refs |
|-----|----------|------|
| 🟠 | **OTP reset no se consume al autenticar**; se otorgan authorities de negocio + se emite **refresh** (sin `ROLE_PASSWORD_RESET_OTP`, pero **con** `ROLE_USER`/`ADMIN`). Access OTP sí se capea a ≤5 min. Quien tiene el código obtiene sesión usable sin completar el PATCH de password. | `RydenAuthenticationProvider:85-90`, `JwtTokenService:61-76` |
| 🟡 | Roles en **access** stale hasta TTL (admin demote tarda hasta refresh) | filter access path |
| 🟡 | Refresh viejo sigue válido hasta `exp` (sin denylist) | diseño JWT típico |
| 🟡 | **GET bytes galería** solo `requireCarExists` — no `requireViewableCar` → fotos de no-ACTIVE enumerables | `CarPictureController:164-183` |
| 🟡 | Tokens en `localStorage` (XSS → session theft); CSP con `style-src 'unsafe-inline'` | SPA + `WebAuthConfig` |
| ⚪ | `web.ignoring()` estáticos — sin security headers en `/public` (cache filter sí corre) | `WebAuthConfig:100-103` |
| ⚪ | Host header en Link `authenticated-user` absoluto | `JwtTokenService.buildUserUri` |

---

## 6. SPA

### 6.1 Fortalezas

- Cliente hypermedia único (`frontend/src/api/client.ts`): `follow`, link collections, MIME vendor, sin `application/json` de API.
- Discovery `apiDiscovery.ts` + `clientConfig`.
- Auth: Basic al índice; refresh proactivo por `exp` + retry 401 con Bearer refresh.
- i18n es/en con test de paridad; rutas SPA centralizadas (`paths.ts`); feature slices.
- Contract tests: openapi / hypermedia / jwt / multipart / i18n.
- Cero `fetch` dispersos en features (salvo XHR de chat upload).

### 6.2 Hallazgos (purismo cliente)

| Sev | Hallazgo | Refs |
|-----|----------|------|
| 🟠 | **URN API desde ID de ruta** (bookmarks / F5 / lista sin `state`): `/cars/${id}`, `/users/${id}`, `/reservations/${id}`, `/reservations/${id}/messages` | `browse/hooks.ts` (`useCar`), `owner/api.ts` (`carUri`), `PublicProfilePage`, `ReservationDetailPage`, `ReservationConfirmationPage`, `NewReservationPage`, `AdminReservationChatPage` |
| 🟡 | Lista de reservas navega por path SPA **sin** pasar `links.self` en `location.state` | `ReservationListCard` |
| 🟡 | Fallbacks de colecciones en discovery si el índice falla | `apiDiscovery.ts` |
| 🟡 | Chat XHR duplica lógica de refresh | `chatUpload.ts` |
| ⚪ | Derivación `${favorites}/${carId}` desde link padre (menos grave) | browse/profile favorites |

> Nota: `paths.ts` construye rutas **del router**, no de la API — eso está bien. El problema es usar la misma forma como URN de `sessionClient.get/follow`.

---

## 7. Controllers: sin lógica de negocio ni validaciones manuales

### 7.1 Inventario (24 controllers)

| Métrica | Resultado |
|---------|-----------|
| Acceso a DAO / EM | **0** |
| `new Car|User|Reservation|…` | **0** |
| `@PreAuthorize` en mutaciones sensibles | Mayoría sí |
| Bean Validation (`@Valid` / `FormValidationSupport` / constraints custom) | Writes JSON/multipart principales |

### 7.2 Qué queda en el controller (aceptable vs olor)

| Tipo | Ejemplos | Veredicto |
|------|----------|-----------|
| Mapeo HTTP ↔ service (DTOs, URI, status codes) | todos | ✅ |
| Routing por query (`ownerId`, `status=all`) | `CarController.listCars` | ✅ documentado; oral |
| Gates imperativos cuando `@PreAuthorize` no alcanza | `BrandController.requireAdmin` si `validated=false`; OTP password en `UserController` | ✅ justificado |
| Validación manual de multipart vacío | `CarPictureController` bytes null/empty → `CarValidationException` | 🟡 preferible Bean Validation / support |
| Parse/sanitización de sort/filtros | `ReservationController` helpers | 🟡 borde web vs service |
| `CarController` / `ReservationController` / `UserController` grosor | 617 / 403 / 330 LOC | ⚪ extraer más a `*Support` (ya hay varios) |

**Comparado con devoluciones a otros grupos:** no se repite el error grave “controllers con lógica de negocio / inicializan modelos”. La validación de entrada está mayormente externalizada a forms + constraints + `FormValidationSupport`.

---

## 8. Contraste con MenuMate y correcciones de otros TPs

### 8.1 MenuMate (referencia nota 9) — qué copiar / qué superar

| Tema | MenuMate | Ryden |
|------|----------|-------|
| Refresh Bearer autentica + rota | Sí (`JwtTokenFilter`) | Sí (paridad) |
| Vendor MIME granulares | Sí | Sí (más granulares + contract tests) |
| MIME-como-operación (`…activate.v1`, password MIME separados) | Presente (anti-patrón de devoluciones) | Evitado: un PATCH user / status |
| Hypermedia discovery índice | Más débil / menos discovery SPA | Más fuerte (`apiDiscovery` + links) |
| `/me` | — | Ausente (bien) |
| Session tracking disable | Verificar en su web.xml | Presente en Ryden |

**Conclusión:** no hace falta “parecerse más a MenuMate” en MIME de escritura; Ryden ya evita varios castigos que MenuMate arriesga. Sí conviene imitar su **claridad de refresh** (ya lograda) y mantener oral listo para zonas grises.

### 8.2 Temas recurrentes en correcciones ajenas → estado Ryden

| Corrección típica | Ryden |
|-------------------|-------|
| Cache busting sin immutable | OK |
| CORS localhost en prod | OK (solo local) |
| Header `Authorization` en login response | OK (X-Access/X-Refresh) |
| Refresh token en **cada** response | OK (solo login/refresh) |
| Refresh en request dedicado | OK (retry mismo request) |
| Entidades sin hipervínculos / links vacíos | Cuidado residual: client-side URN inventada |
| Controllers con business logic | OK |
| Paginación en body / COUNT en memoria | OK en SQL; **excepto** BookableBrowse |
| Email/admin públicos | Mitigado (vistas MIME) |
| JSESSIONID | OK |
| Filtros no impactan URL / history | SPA paths + search params — revisar oral demos |
| IDs sueltos en body en vez de URN | Mejorado (reviews, neighborhoodUri, reservationUri); favorites aún derivan id |

---

## 9. Plan de acción priorizado

### P0 — antes del oral / riesgo alto

1. **OTP reset:** no emitir refresh con roles de negocio; access reset-only (o consumir OTP al Basic); alinear con claim `pwd_reset` de FINAL_PAW_V1.
2. **SPA:** pasar `links.self` (y `messages`) en `location.state` desde listas; en detalle usar solo esa URN; eliminar `carUri(id)` / `` `/reservations/${id}` `` como camino feliz.
3. **Galería bytes:** `requireViewableCar` (o equivalente) en `GET …/pictures/primary` y `/{pictureId}`.
4. **BookableBrowseSupport:** empujar predicado bookable a SQL / keyset; no materializar todo el catálogo.

### P1 — calidad / defensa oral

5. Documentar en `openapi.yaml` el `DELETE` de availabilities por rango (o renombrar modelo).
6. Decidir defensa de plate-in-same-MIME vs MIME `car.private`.
7. Alinear enum de `sort` reservations contrato ↔ sanitizer.
8. `ReservationPaymentServiceImplTest`.
9. Factory estática en `OtpAttemptLimiter`; justificar bulk DELETE OTP en PR/oral.

### P2 — higiene

10. Reducir grosor de `CarController` moviendo branches a supports.
11. `hbm2ddl.auto=none` fuera de local.
12. Limpiar restos de clases session/MVC si quedan sin uso.

---

## 10. Matriz de cobertura de esta auditoría

| Módulo / capa | Cubierto |
|---------------|----------|
| `models` (entities, DTOs, equality, timezone) | Sí |
| `persistence` / contracts (N+1, locks, bulk, paginación) | Sí |
| `services` / contracts (TX, schedulers, mail, workflow) | Sí |
| `webapp` controllers (24), security, filters, web.xml, OpenAPI | Sí |
| `frontend` api/session/features/routes/i18n/tests | Sí |
| Docs cátedra + correcciones + MenuMate + auditorías previas | Sí |

---

## 11. Veredicto final

El proyecto está en **estado defendible para final** si se cierran los P0 (OTP reset, URNs del SPA, ACL de galería, browse en memoria) y se preparan las zonas grises REST. La base (stateless JWT, hypermedia, MIME, cache, capas, tests de contrato) es **sólida y superior a muchos TPs corregidos negativamente**. MenuMate sirve de referencia de auth refresh, no de modelo a copiar en MIME de escritura.

*Fin de la auditoría. Archivo: `nueva-auditoria.md` (raíz del repo).*

---

## 12. Addendum (subagentes — tarde 16-jul)

Hallazgos adicionales tras el barrido paralelo; complementan §§4–7 sin reabrir P0 ya cerrados.

| Sev | Hallazgo | Fuente |
|-----|----------|--------|
| 🟠 | **OpenAPI desfasado del código:** `GET /cars` (Accept vs `status=all`), `POST /reviews` (query `reservationId` vs multipart `reservationUri`), `UserDto` público con KYC/`documents` en YAML pero no en Java, params `flexible*` / `priceMarket` en index | [REST controllers](20cac2f2-81c5-4e53-8c66-5bdd9b5eff52), [REST completo](a280727a-b373-4536-89d4-00185c187d9a) |
| 🟠 | **`links.self` de segmentos mensuales no dereferenciable:** apunta a `?from&until` pero GET solo usa `month` | `AvailabilityDto`, `CarAvailabilityController` |
| 🟠 | **SPA aún arma `/documents`** pese a que el server quitó `links.documents` | `profile/api.ts`, `owner/api.ts`, `reservations/api.ts` |
| 🟡 | **404/400 vacíos** en binarios (evitan mappers `error.v1+json`) | `CarPictureController`, receipts, `ImageController`, etc. |
| 🟡 | **Seguro:** errores en `X-Ryden-Error` + body vacío | `CarInsuranceController` |
| 🟡 | **`price-insight`:** stats pueden no corresponder al modelo del path | `ModelCatalogSupport` |
| 🟡 | **Lifecycle:** wrappers en `ReservationServiceImpl` siguen con `@Transactional` (emails batch) | [Funcionamiento vigente](51239888-6795-4f16-9eb2-7c3da444b7e6) |
| 🟡 | **Chat/review uploads** sin `BinaryPayloadSupport` (`readAllBytes`) | `ReservationMessageController`, `ReviewSubmitSupport` |
| ⚪ | **Effective Java:** `AdminServiceImpl` tiene `@Transactional` de clase — el riesgo admin es checklist/orquestación multi-peer, no “sin TX” | Corregir lectura de [Effective Java](ea932433-8bbd-4a26-848d-1b1084fb1c09) |
| ⚪ | **Doble reserva:** lock `lockForReservationWrite` confirmado — el informe funcional inicial lo marcó alto por error | Re-verificado en código |

**Corpus:** guía en `context/FINAL_PAW_V1/*` + correcciones en `context/correcciones-finales-paw/` — ver [Buscar docs](1706ab42-901e-4120-aa6f-c51f5987b923).

### Effective Java (re-verificación) — [Auditar EJ vigente](dbfeed13-3c21-483b-860f-18f8b701e450)

| Sev | Hallazgo |
|-----|----------|
| 🟡 | `OtpAttemptLimiter`: 2 constructores públicos |
| 🟡 | `FavCar` / `ReservationAvailabilityCoverage`: `equals` usa `this.id` (campo) |
| 🟡 | `Page` / upload DTOs: sin copia defensiva del `List`/`byte[]` |
| 🟡 | `CarAvailabilityController` instancia `AvailabilityPeriod` (VO de dominio) |
| ⚪ | `@Transactional` / self-invoke / bulk OTP: mayormente justificados |

### vs MenuMate — [Comparar MenuMate](8da5e921-99fb-4fcc-9332-0633488a3908)

**TP-PAW por delante** en: SecurityFilterChain moderno, ETag en binarios DB, cache `/public` vs `index.html`, HypermediaClient + discovery, `user.private`, 401 sin prompt nativo, refresh que recarga roles desde DB.

**Vigilar en ambos (cátedra):** refresh como credencial de autenticación de cualquier request (replay); MIME/campos que rutean PATCH.

**MenuMate más limpio** en: simplicidad de controllers/DTOs (menos `*Support`).
