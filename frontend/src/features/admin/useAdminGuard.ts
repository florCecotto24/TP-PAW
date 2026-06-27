import { useSessionStore } from '../../session/sessionStore';

/**
 * Estado de acceso al área admin. Las vistas se muestran SOLO si el usuario
 * logueado tiene `role === 'admin'` (UserDto). Mientras el store todavía no
 * resolvió el usuario (rehidratación de tokens), reportamos `loading` para no
 * mostrar un "no autorizado" prematuro.
 */
export interface AdminGuard {
  isAdmin: boolean;
  loading: boolean;
}

export function useAdminGuard(): AdminGuard {
  const currentUser = useSessionStore((s) => s.currentUser);
  const status = useSessionStore((s) => s.status);

  // Tenemos tokens pero todavía no el UserDto: seguimos cargando.
  const loading = status === 'authenticating' || (status === 'authenticated' && currentUser == null);

  return {
    isAdmin: currentUser?.role === 'admin',
    loading,
  };
}
