/**
 * Lectura liviana del payload JWT (sin verificar firma).
 * Solo se usa para decidir si conviene renovar el access token antes de un request.
 */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/** True si el token tiene `exp` y ya venció (con margen opcional). Sin `exp` legible, false. */
export function isAccessTokenExpired(token: string, skewSeconds = 30): boolean {
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== 'number') return false;
  const nowSec = Math.floor(Date.now() / 1000);
  return nowSec + skewSeconds >= payload.exp;
}
