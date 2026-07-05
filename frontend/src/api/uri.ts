// Helpers de URN/URI compartidos.
// -----------------------------------------------------------------------------

import { appBasePath } from '../appBasePath';

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
    const base = appBasePath();
    if (base && typeof window !== 'undefined') {
      try {
        const parsed = new URL(uri);
        if (parsed.origin === window.location.origin) {
          if (!parsed.pathname.startsWith(`${base}/`) && parsed.pathname !== base) {
            parsed.pathname = joinBaseAndPath(base, parsed.pathname);
          } else {
            parsed.pathname = collapseDuplicateSlashes(parsed.pathname);
          }
          return parsed.toString();
        }
      } catch {
        /* keep original */
      }
    }
    return uri;
  }
  const base = appBasePath();
  const path = uri.startsWith('/') ? uri : `/${uri}`;
  return joinBaseAndPath(base, path);
}

/** Resuelve una URN relativa de la API (p.ej. `/image/3`) a URL absoluta para `<img src>`. */
export function apiAssetUrl(uri: string): string {
  return resolveApiUrl(uri);
}
