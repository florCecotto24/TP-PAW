import { isAccessTokenExpired } from './jwt';
import { MediaTypes } from './mediaTypes';
import type { ErrorDto, Links, PageLinks } from './types';
import { resolveApiUrl } from './uri';

// =============================================================================
// Cliente hypermedia de Ryden
// -----------------------------------------------------------------------------
// Capa fina sobre `fetch` que implementa el contrato de la API (LINEAMIENTOS):
//   - §1.7  setea Accept/Content-Type con los vendor MIME `vnd.paw.*`.
//   - §1.8  auth stateless por header Authorization:
//             * "login" = Basic base64(email:password) a un endpoint neutro;
//               la respuesta trae X-Access-Token / X-Refresh-Token y un header
//               Link rel="authenticated-user" con la URN del usuario logueado.
//             * requests normales = Bearer <accessToken>.
//             * si el access ya expiró (lectura del claim `exp`), renueva antes del
//               request con Bearer <refreshToken> a {@link AUTH_PROBE_PATH}.
//             * ante 401, reintenta UNA vez con Bearer <refreshToken> al MISMO
//               endpoint; la respuesta trae un par nuevo de tokens en headers.
//   - §1.6  paginación SOLO por header `Link` (RFC 5988) + X-Total-Count.
//   - hipervínculos: se navega por los `links` de los DTOs (follow), no se
//     construyen URLs a mano.
//
// El estado de tokens NO vive acá: se inyecta por callbacks (getAccessToken /
// getRefreshToken / onTokens / onAuthenticatedUser) para que el sessionStore
// (zustand) sea la única fuente de verdad y este cliente quede testeable.
// =============================================================================

export interface TokenAccessors {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  /** Persiste un par de tokens recién emitido (post-Basic o post-refresh). */
  onTokens: (access: string, refresh: string) => void;
  /** Reporta la URN del usuario logueado (Link rel="authenticated-user"). */
  onAuthenticatedUser?: (userUri: string) => void;
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface RequestOptions {
  method?: HttpMethod;
  /** MIME that se manda en `Accept` (lectura). Default: ninguno. */
  accept?: string;
  /** MIME que se manda en `Content-Type` (escritura JSON). */
  contentType?: string;
  /** Cuerpo: objeto (se serializa a JSON) o un BodyInit ya armado (multipart). */
  body?: unknown;
  /** Query params; arrays append repeated keys. */
  query?: Record<string, string | number | boolean | string[] | undefined | null>;
  /** Si true, no agrega Authorization (endpoints anónimos: registro, etc.). */
  anonymous?: boolean;
  /** Authorization explícito (p.ej. Basic en login). Tiene prioridad. */
  authorization?: string;
  /** Headers adicionales (p.ej. nombre de archivo en uploads binarios). */
  extraHeaders?: Record<string, string>;
  /** Habilita/inhabilita el reintento 401→refresh (default: true). */
  retryOnUnauthorized?: boolean;
}

/** Respuesta tipada: cuerpo parseado + metadata de paginación + status. */
export interface ApiResponse<T> {
  data: T;
  status: number;
  page: PageLinks;
  /** Header Location (creaciones 201). */
  location: string | null;
  /** Acceso crudo a headers por si una feature lo necesita. */
  headers: Headers;
}

/** Error de API que conserva status + ErrorDto para mapear a i18n. */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly body?: ErrorDto;

  constructor(status: number, body?: ErrorDto) {
    super(body?.message ?? `API error ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.code = body?.code;
    this.body = body;
  }
}

/**
 * Detecta FormData aunque `instanceof` falle (p.ej. otro realm / iframe).
 * Sin esto, un Content-Type forzado sin boundary hace que Jersey responda 400 genérico.
 */
export function isFormDataBody(body: unknown): boolean {
  if (body == null || typeof body !== 'object') return false;
  if (typeof FormData !== 'undefined' && body instanceof FormData) return true;
  return Object.prototype.toString.call(body) === '[object FormData]';
}

// Endpoint "neutro" para autenticar con Basic / renovar con refresh.
// La API NO tiene /me ni /login; el descriptor de la API en `/` (§1.10) es el
// endpoint liviano y público recomendado por openapi.yaml para esto: devuelve
// los tokens en headers y el Link rel="authenticated-user". Decisión D2.
export const AUTH_PROBE_PATH = '/api';

const REL_AUTHENTICATED_USER = 'authenticated-user';

/**
 * Parsea un header `Link` (RFC 5988): `<url>; rel="next", <url>; rel="last"`.
 * Devuelve un mapa rel -> url. Tolerante a espacios y comillas opcionales.
 */
export function parseLinkHeader(headerValue: string | null): Record<string, string> {
  const out: Record<string, string> = {};
  if (!headerValue) return out;

  for (const part of headerValue.split(',')) {
    const segment = part.trim();
    if (!segment) continue;
    const match = segment.match(/^<([^>]*)>\s*;\s*(.+)$/);
    if (!match) continue;
    const url = match[1];
    const relMatch = match[2].match(/rel\s*=\s*"?([^";]+)"?/i);
    if (!relMatch) continue;
    // Un mismo Link puede declarar varios rels separados por espacio.
    for (const rel of relMatch[1].trim().split(/\s+/)) {
      out[rel.toLowerCase()] = url;
    }
  }
  return out;
}

/** Extrae los links de paginación + total del header Link / X-Total-Count. */
export function extractPageLinks(headers: Headers): PageLinks {
  const rels = parseLinkHeader(headers.get('Link'));
  const totalRaw = headers.get('X-Total-Count');
  // Un header vacío ("") daría Number('') === 0 (no NaN) y reportaría 0 resultados
  // en una página no vacía: lo tratamos como ausente.
  const total = totalRaw != null && totalRaw.trim() !== '' ? Number(totalRaw) : undefined;
  return {
    first: rels.first,
    prev: rels.prev,
    next: rels.next,
    last: rels.last,
    total: Number.isNaN(total) ? undefined : total,
  };
}

/**
 * Recorre {@code res.page.next} hasta reunir todas las páginas de una colección paginada.
 * Útil cuando la UI necesita el conjunto completo (p.ej. galería acotada del auto).
 */
export async function followAllPages<T>(
  client: Pick<HypermediaClient, 'follow'>,
  startLink: string,
  opts: RequestOptions = {},
): Promise<ApiResponse<T[]>> {
  const all: T[] = [];
  let url: string | undefined = startLink;
  let lastRes!: ApiResponse<T[]>;
  while (url) {
    lastRes = await client.follow<T[]>(url, opts);
    all.push(...(lastRes.data ?? []));
    url = lastRes.page.next;
  }
  return { ...lastRes, data: all };
}

/**
 * Resolve link-only API collections (pattern A in AGENTS.md / openapi.yaml).
 * Intentional HTTP N+1: collection returns Links[], then one GET per self.
 */
export async function followLinkCollection<T>(
  client: Pick<HypermediaClient, 'follow'>,
  collectionRes: ApiResponse<Links[]>,
  itemAccept: string,
): Promise<T[]> {
  const selfLinks = (collectionRes.data ?? []).map((item) => item.self).filter(Boolean);
  if (selfLinks.length === 0) {
    return [];
  }
  const settled = await Promise.allSettled(
    selfLinks.map((href) => client.follow<T>(href, { accept: itemAccept })),
  );
  return settled
    .filter((result): result is PromiseFulfilledResult<ApiResponse<T>> => result.status === 'fulfilled')
    .map((result) => result.value.data)
    .filter((item): item is T => item != null);
}

/** GET a (possibly paginated) link collection and resolve item representations. */
export async function getLinkCollectionPage<TItem>(
  client: Pick<HypermediaClient, 'follow'>,
  pathOrLink: string,
  opts: {
    collectionAccept: string;
    itemAccept: string;
    query?: RequestOptions['query'];
  },
): Promise<ApiResponse<TItem[]>> {
  const collectionRes = await client.follow<Links[]>(pathOrLink, {
    accept: opts.collectionAccept,
    query: opts.query,
  });
  const data = await followLinkCollection<TItem>(client, collectionRes, opts.itemAccept);
  return { ...collectionRes, data };
}

export class HypermediaClient {
  /** Serializa renovaciones concurrentes cuando el access ya expiró. */
  private refreshInFlight: Promise<void> | null = null;

  constructor(private readonly tokens: TokenAccessors) {}

  /**
   * Renueva el par de tokens con el refresh antes del request si el access venció.
   * Evita mandar Bearer expirado (ruido en server) y el roundtrip 401→refresh.
   */
  private async ensureFreshAccessToken(): Promise<void> {
    const access = this.tokens.getAccessToken();
    const refresh = this.tokens.getRefreshToken();
    if (!access || !refresh || !isAccessTokenExpired(access)) {
      return;
    }
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.performTokenRefresh().finally(() => {
        this.refreshInFlight = null;
      });
    }
    await this.refreshInFlight;
  }

  /** GET {@link AUTH_PROBE_PATH} con Bearer refresh; absorbe X-Access-Token / X-Refresh-Token. */
  private async performTokenRefresh(): Promise<void> {
    const refresh = this.tokens.getRefreshToken();
    if (!refresh) return;

    const url = this.buildUrl(AUTH_PROBE_PATH);
    const res = await fetch(
      url,
      this.buildInit(
        { method: 'GET', accept: MediaTypes.api, retryOnUnauthorized: false },
        `Bearer ${refresh}`,
      ),
    );
    this.absorbAuthHeaders(res.headers);
  }

  // --- Lectura de headers de auth tras CUALQUIER respuesta -------------------
  private absorbAuthHeaders(headers: Headers): void {
    const access = headers.get('X-Access-Token');
    const refresh = headers.get('X-Refresh-Token');
    if (access && refresh) {
      this.tokens.onTokens(access, refresh);
    }
    const authUser = parseLinkHeader(headers.get('Link'))[REL_AUTHENTICATED_USER];
    if (authUser && this.tokens.onAuthenticatedUser) {
      this.tokens.onAuthenticatedUser(authUser);
    }
  }

  private appendQueryParam(usp: URLSearchParams, key: string, value: string | number | boolean | string[]): void {
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined && item !== null && String(item) !== '') {
          usp.append(key, String(item));
        }
      }
      return;
    }
    if (value !== undefined && value !== null) {
      usp.append(key, String(value));
    }
  }

  private buildUrl(path: string, query?: RequestOptions['query']): string {
    if (path.startsWith('http://') || path.startsWith('https://')) {
      const url = new URL(path);
      if (query) {
        for (const [k, v] of Object.entries(query)) {
          if (v === undefined || v === null) continue;
          this.appendQueryParam(url.searchParams, k, v);
        }
      }
      const resolvedBase = resolveApiUrl(`${url.origin}${url.pathname}`);
      const out = new URL(resolvedBase);
      out.search = url.search;
      if (url.hash) out.hash = url.hash;
      return out.toString();
    }
    const absolutePath = resolveApiUrl(path.startsWith('/') ? path : `/${path}`);
    if (!query) return absolutePath;
    const usp = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v === undefined || v === null) continue;
      this.appendQueryParam(usp, k, v);
    }
    const qs = usp.toString();
    return qs ? `${absolutePath}${absolutePath.includes('?') ? '&' : '?'}${qs}` : absolutePath;
  }

  private buildInit(opts: RequestOptions, authorization: string | null): RequestInit {
    const headers = new Headers();
    if (opts.accept) headers.set('Accept', opts.accept);

    let body: BodyInit | undefined;
    let isFormData = false;
    if (opts.body !== undefined && opts.body !== null) {
      isFormData = isFormDataBody(opts.body);
      if (
        isFormData
        || opts.body instanceof Blob
        || (typeof File !== 'undefined' && opts.body instanceof File)
        || typeof opts.body === 'string'
        || opts.body instanceof ArrayBuffer
        || ArrayBuffer.isView(opts.body)
      ) {
        body = opts.body as BodyInit;
        // FormData: el browser debe setear multipart + boundary. Si forzamos
        // Content-Type (aunque sea multipart/form-data) Jersey responde 400
        // genérico ("HTTP 400 Bad Request", ~68 bytes) por falta de boundary.
        // Bodies ya serializados (Blob/bytes/string) sí pueden llevar contentType
        // (p.ej. multipart armado a mano con boundary explícito).
        if (!isFormData && opts.contentType) {
          headers.set('Content-Type', opts.contentType);
        }
      } else {
        body = JSON.stringify(opts.body);
        // Escrituras JSON: vendor MIME, nunca application/json.
        headers.set('Content-Type', opts.contentType ?? opts.accept ?? MediaTypes.user);
      }
    }

    if (authorization) headers.set('Authorization', authorization);
    if (opts.extraHeaders) {
      for (const [name, value] of Object.entries(opts.extraHeaders)) {
        headers.set(name, value);
      }
    }
    // Defensa final: nunca dejar Content-Type en FormData (ni vía extraHeaders).
    if (isFormData) {
      headers.delete('Content-Type');
    }

    return { method: opts.method ?? 'GET', headers, body };
  }

  private async toApiResponse<T>(res: Response): Promise<ApiResponse<T>> {
    let data: unknown = undefined;
    if (res.status !== 204 && res.status !== 205) {
      const text = await res.text();
      if (text) {
        try {
          data = JSON.parse(text);
        } catch {
          data = text;
        }
      }
    }

    if (!res.ok) {
      throw new ApiError(res.status, (data as ErrorDto) ?? undefined);
    }

    return {
      data: data as T,
      status: res.status,
      page: extractPageLinks(res.headers),
      location: res.headers.get('Location'),
      headers: res.headers,
    };
  }

  /**
   * Arma el request, agrega Authorization, absorbe tokens de la respuesta y, ante 401,
   * reintenta UNA vez con el refresh token — sobre el mismo request, sin roundtrip dedicado
   * (§1.8). Devuelve la `Response` cruda: la capa de arriba decide cómo leer el body
   * (JSON/texto vía `toApiResponse`, o binario vía `.blob()` en {@link getBlob}).
   */
  private async fetchWithRetry(path: string, opts: RequestOptions, method: HttpMethod): Promise<Response> {
    const url = this.buildUrl(path, opts.query);
    const requestOpts = { ...opts, method };

    const explicitAuth = opts.authorization ?? null;
    if (!explicitAuth && !opts.anonymous) {
      await this.ensureFreshAccessToken();
    }

    const access = opts.anonymous ? null : this.tokens.getAccessToken();
    const firstAuth = explicitAuth ?? (access ? `Bearer ${access}` : null);

    const res = await fetch(url, this.buildInit(requestOpts, firstAuth));
    this.absorbAuthHeaders(res.headers);

    // Reintento 401 -> refresh (solo si no fue un intento explícito/anónimo).
    const canRetry =
      res.status === 401 &&
      opts.retryOnUnauthorized !== false &&
      !explicitAuth &&
      !opts.anonymous;

    if (canRetry) {
      const refresh = this.tokens.getRefreshToken();
      if (refresh) {
        const retryRes = await fetch(url, this.buildInit(requestOpts, `Bearer ${refresh}`));
        this.absorbAuthHeaders(retryRes.headers);
        return retryRes;
      }
    }

    return res;
  }

  /** Núcleo del cliente para respuestas JSON/texto (ver {@link fetchWithRetry}). */
  async request<T = unknown>(path: string, opts: RequestOptions = {}): Promise<ApiResponse<T>> {
    const res = await this.fetchWithRetry(path, opts, opts.method ?? 'GET');
    return this.toApiResponse<T>(res);
  }

  /**
   * GET de un recurso binario autenticado (documento KYC, comprobante, adjunto de chat) con el
   * mismo manejo de `Authorization` + reintento 401→refresh que {@link request}, pero devolviendo
   * un `Blob` en vez de intentar parsear el body como JSON/texto (`toApiResponse` asume texto y
   * rompería con bytes binarios). Reemplaza los `fetch()` manuales con Bearer a mano que había
   * repetidos en cada feature (perfil, reservas, admin) para "abrir en pestaña nueva" — quedan sin
   * el reintento de refresh que sí tiene cualquier otro request del cliente central.
   * Devuelve `null` si la respuesta no es 2xx (404, 401 sin refresh disponible, etc.).
   */
  async getBlob(path: string, opts: RequestOptions = {}): Promise<Blob | null> {
    const download = await this.getBlobDownload(path, opts);
    return download?.blob ?? null;
  }

  /**
   * Igual que {@link getBlob}, pero conserva el nombre sugerido del header
   * {@code Content-Disposition} cuando el server lo envía.
   */
  async getBlobDownload(
    path: string,
    opts: RequestOptions = {},
  ): Promise<{ blob: Blob; fileName?: string } | null> {
    const res = await this.fetchWithRetry(path, opts, 'GET');
    if (!res.ok) return null;
    const blob = await res.blob();
    const fileName = parseContentDispositionFileName(res.headers.get('Content-Disposition'));
    return { blob, fileName };
  }

  // --- Helpers por verbo -----------------------------------------------------
  get<T = unknown>(path: string, opts: RequestOptions = {}) {
    return this.request<T>(path, { ...opts, method: 'GET' });
  }
  post<T = unknown>(path: string, body?: unknown, opts: RequestOptions = {}) {
    return this.request<T>(path, { ...opts, method: 'POST', body });
  }
  put<T = unknown>(path: string, body?: unknown, opts: RequestOptions = {}) {
    return this.request<T>(path, { ...opts, method: 'PUT', body });
  }
  patch<T = unknown>(path: string, body?: unknown, opts: RequestOptions = {}) {
    return this.request<T>(path, { ...opts, method: 'PATCH', body });
  }
  del<T = unknown>(path: string, opts: RequestOptions = {}) {
    return this.request<T>(path, { ...opts, method: 'DELETE' });
  }

  /** Navega un hipervínculo (de un bloque `links`) sin armar URLs a mano. */
  follow<T = unknown>(link: string, opts: RequestOptions = {}) {
    return this.request<T>(link, opts);
  }

  /**
   * "Login": manda Authorization: Basic base64(email:password) al endpoint
   * neutro `/`. La respuesta trae X-Access-Token / X-Refresh-Token (absorbidos
   * por absorbAuthHeaders -> onTokens) y el Link rel="authenticated-user"
   * (-> onAuthenticatedUser). No existe /login ni /me (§1.8, decisión D2).
   */
  async loginBasic(email: string, password: string): Promise<void> {
    const basic = `Basic ${encodeBase64(`${email}:${password}`)}`;
    await this.request(AUTH_PROBE_PATH, {
      method: 'GET',
      accept: MediaTypes.api,
      authorization: basic,
      retryOnUnauthorized: false,
    });
  }
}

/** base64 de un string UTF-8 (browser/jsdom: btoa opera sobre Latin1, codificamos UTF-8 primero). */
export function encodeBase64(value: string): string {
  const utf8 = new TextEncoder().encode(value);
  let binary = '';
  for (const byte of utf8) binary += String.fromCharCode(byte);
  return btoa(binary);
}

export function parseContentDispositionFileName(header: string | null): string | undefined {
  if (!header) return undefined;
  const utf8 = /filename\*=UTF-8''([^;\n]+)/i.exec(header);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1]);
    } catch {
      return utf8[1];
    }
  }
  const quoted = /filename="([^"]+)"/i.exec(header);
  if (quoted?.[1]) return quoted[1];
  const plain = /filename=([^;\n]+)/i.exec(header);
  if (plain?.[1]) return plain[1].trim();
  return undefined;
}
