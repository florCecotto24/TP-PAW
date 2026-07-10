import { useSessionStore } from '../../session/sessionStore';

// =============================================================================
// Hooks compartidos del área PROFILE.
// =============================================================================

/**
 * URN del usuario logueado (p.ej. "/users/5"), descubierta vía
 * Link rel="authenticated-user" (decisión D2). Null si anónimo.
 */
export function useMyUserUri(): string | null {
  const userUri = useSessionStore((s) => s.currentUserUri);
  const self = useSessionStore((s) => s.currentUser?.links?.self);
  return userUri ?? self ?? null;
}

/** Id numérico propio extraído de la URN, o null. Útil para comparaciones. */
export function useMyUserId(): string | null {
  const uri = useMyUserUri();
  if (!uri) return null;
  return uri.split('/').filter(Boolean).pop() ?? null;
}

/**
 * Id numérico extraído de cualquier URN (".../users/5" → "5").
 *
 * NOTA: NO se consolida con `api/uri#idFromUri` porque su semántica difiere:
 * esta variante NO descarta query string (toma el último segmento crudo). Las
 * entradas reales son URNs limpias (currentUserUri / links.self sin query), pero
 * para no cambiar comportamiento sutilmente se la deja local.
 */
export function idFromUri(uri: string | undefined | null): string | null {
  if (!uri) return null;
  return uri.split('/').filter(Boolean).pop() ?? null;
}
