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
