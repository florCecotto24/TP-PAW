// Acceso al usuario actual derivado del sessionStore.
// La API no expone id propio: lo derivamos de la URN `currentUserUri`
// (Link rel="authenticated-user", p.ej. /users/42), decisión D2 del cliente.
import { useSessionStore } from '../../session/sessionStore';
import { idFromUri } from './api';

export interface CurrentUser {
  id: string | null;
  role: 'user' | 'admin' | null;
  isAuthenticated: boolean;
}

export function useCurrentUser(): CurrentUser {
  const userUri = useSessionStore((s) => s.currentUserUri);
  const user = useSessionStore((s) => s.currentUser);
  return {
    id: idFromUri(userUri),
    role: user?.role ?? null,
    isAuthenticated: Boolean(userUri),
  };
}
