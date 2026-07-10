// Helpers de URN/URI compartidos.
// -----------------------------------------------------------------------------

import { appBasePath } from '../appBasePath';

/** JAX-RS servlet filter mount point ({@code web.xml} {@code /api/*}). */
export const API_BASE = '/api';

export function apiBasePath(): string {
  return joinBaseAndPath(appBasePath(), API_BASE);
}

function pathUnderApi(pathname: string, ctx: string): boolean {
  const apiRoot = joinBaseAndPath(ctx, API_BASE);
  return (
    pathname === apiRoot
    || pathname.startsWith(`${apiRoot}/`)
    || (ctx === '' && (pathname === API_BASE || pathname.startsWith(`${API_BASE}/`)))
  );
}

/** Ensures a servlet path includes the {@link API_BASE} segment (Jersey lives under /api/*). */
function ensureApiPath(pathname: string, ctx: string): string {
  const collapsed = collapseDuplicateSlashes(pathname);
  if (pathUnderApi(collapsed, ctx)) {
    return collapsed;
  }
  const apiRoot = joinBaseAndPath(ctx, API_BASE);
  if (collapsed === API_BASE || collapsed === `${API_BASE}/`) {
    return apiRoot;
  }
  if (ctx && (collapsed === ctx || collapsed.startsWith(`${ctx}/`))) {
    const suffix = collapsed.slice(ctx.length) || '/';
    if (suffix === API_BASE || suffix === `${API_BASE}/`) {
      return apiRoot;
    }
    return joinBaseAndPath(apiRoot, suffix);
  }
  return joinBaseAndPath(apiRoot, collapsed.startsWith('/') ? collapsed : `/${collapsed}`);
}

/**
 * Último segmento "limpio" de una URN (p.ej. "/users/42?x=1/" -> "42").
 * Descarta query string y barras finales. Devuelve "" si no hay segmento.
 * Usar cuando el caller asume que SIEMPRE hay un self (no admite ausencia).
 */
export function lastPathSegment(uri: string): string {
  const clean = uri.split('?')[0].replace(/\/+$/, '');
  return clean.substring(clean.lastIndexOf('/') + 1);
}

/**
 * Igual que {@link lastPathSegment} pero tolerante a la ausencia de URN:
 * devuelve `null` si la URN es nula/vacía o no tiene segmento. Usar cuando el
 * link es opcional (links.* que no toda vista trae).
 */
export function idFromUri(uri: string | undefined | null): string | null {
  if (!uri) return null;
  return lastPathSegment(uri) || null;
}

/** Colapsa barras duplicadas en un path (Spring StrictHttpFirewall rechaza {@code //}). */
export function collapseDuplicateSlashes(pathname: string): string {
  return pathname.replace(/\/{2,}/g, '/');
}

/** Une context path + URN sin duplicar prefijo ni generar {@code //}. */
export function joinBaseAndPath(base: string, path: string): string {
  const normalizedBase = base.replace(/\/+$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  if (!normalizedBase) {
    return collapseDuplicateSlashes(normalizedPath);
  }
  if (normalizedPath === normalizedBase || normalizedPath.startsWith(`${normalizedBase}/`)) {
    return collapseDuplicateSlashes(normalizedPath);
  }
  return collapseDuplicateSlashes(`${normalizedBase}${normalizedPath}`);
}

/**
 * Resuelve una URN de la API (relativa o absoluta) a URL usable en fetch / `<img src>`.
 * Jersey suele emitir links absolutos; hay que usarlos tal cual (no volver a prefijar el context path).
 * Si el link absoluto omite el context path del WAR (`/webapp`), se corrige cuando el origen coincide.
 */
export function resolveApiUrl(uri: string): string {
  if (!uri) {
    return uri;
  }
  if (uri.startsWith('http://') || uri.startsWith('https://')) {
    const ctx = appBasePath();
    if (typeof window !== 'undefined') {
      try {
        const parsed = new URL(uri);
        if (parsed.origin === window.location.origin) {
          let pathname = parsed.pathname;
          if (ctx && !pathname.startsWith(`${ctx}/`) && pathname !== ctx) {
            pathname = joinBaseAndPath(ctx, pathname);
          } else {
            pathname = collapseDuplicateSlashes(pathname);
          }
          parsed.pathname = ensureApiPath(pathname, ctx);
          return parsed.toString();
        }
      } catch {
        /* keep original */
      }
    }
    return uri;
  }
  const ctx = appBasePath();
  const path = uri.startsWith('/') ? uri : `/${uri}`;
  return ensureApiPath(path, ctx);
}

/** Resuelve una URN relativa de la API (p.ej. `/image/3`) a URL absoluta para `<img src>`. */
export function apiAssetUrl(uri: string): string {
  return resolveApiUrl(uri);
}

/**
 * Cover thumbnail for a car: prefers {@code links.cover} when the API projects it;
 * otherwise falls back to {@code {carSelf}/pictures/primary}.
 */
export function carCoverAssetUrl(
  carSelfUri: string | null | undefined,
  coverLink?: string | null,
): string | null {
  if (coverLink) return apiAssetUrl(coverLink);
  if (!carSelfUri) return null;
  const base = carSelfUri.split('?')[0].replace(/\/$/, '');
  return apiAssetUrl(`${base}/pictures/primary`);
}

/** Profile picture link from a hypermedia `links` block, or null when absent. */
export function profilePictureAssetUrl(
  links: { profilePicture?: string; [rel: string]: string | undefined } | null | undefined,
): string | null {
  const path = links?.profilePicture;
  return path ? apiAssetUrl(path) : null;
}

/** Derives the profile-picture sub-resource URL from a user `self` URN. */
export function profilePictureAssetUrlFromSelf(selfUri: string | null | undefined): string | null {
  if (!selfUri) return null;
  const base = selfUri.split('?')[0].replace(/\/+$/, '');
  return apiAssetUrl(`${base}/profile-picture`);
}

/**
 * Resolves a profile picture for display. Prefers `links.profilePicture`; while the
 * user DTO is still loading, falls back to `{self}/profile-picture` from the session URN.
 */
export function resolveProfilePictureAssetUrl(
  links: { profilePicture?: string; self?: string; [rel: string]: string | undefined } | null | undefined,
  sessionUserUri?: string | null,
): string | null {
  const fromLinks = profilePictureAssetUrl(links);
  if (fromLinks) return fromLinks;
  if (links && !links.profilePicture) return null;
  return profilePictureAssetUrlFromSelf(links?.self ?? sessionUserUri);
}

/**
 * Normalizes a user URN from {@code authenticated-user} (often absolute and missing
 * {@code /api}) to a relative JAX-RS path such as {@code /users/42}.
 */
export function canonicalApiUserPath(uri: string): string {
  const resolved = resolveApiUrl(uri);
  const apiRoot = apiBasePath();
  if (resolved.startsWith('http://') || resolved.startsWith('https://')) {
    try {
      const pathname = new URL(resolved).pathname;
      if (pathname === apiRoot) return '/';
      if (pathname.startsWith(`${apiRoot}/`)) {
        return pathname.slice(apiRoot.length);
      }
      return pathname;
    } catch {
      return uri;
    }
  }
  if (resolved === apiRoot) return '/';
  if (resolved.startsWith(`${apiRoot}/`)) {
    return resolved.slice(apiRoot.length);
  }
  return resolved.startsWith('/') ? resolved : `/${resolved}`;
}
