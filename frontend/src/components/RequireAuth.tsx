import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSessionStore } from '../session/sessionStore';

/**
 * Route guard: replica el comportamiento de Spring Security del proyecto original
 * (las rutas protegidas redirigen al login si no hay sesión). Para rutas de admin
 * exige además el rol ADMIN. Preserva el destino en `state.from` para volver tras
 * loguearse.
 */
export default function RequireAuth({ children, admin = false }: { children: ReactNode; admin?: boolean }) {
  const { t } = useTranslation();
  const status = useSessionStore((s) => s.status);
  const currentUser = useSessionStore((s) => s.currentUser);
  const location = useLocation();

  if (status !== 'authenticated') {
    return <Navigate to="/ingresar" state={{ from: location.pathname + location.search }} replace />;
  }

  if (admin) {
    // Tras un refresh el UserDto se rehidrata async: esperamos a tenerlo antes de
    // decidir, para no expulsar a un admin válido mientras carga.
    if (!currentUser) {
      return <p className="container py-5 text-secondary" role="status">{t('app.loading')}</p>;
    }
    if (currentUser.role !== 'admin') {
      return <Navigate to="/" replace />;
    }
  }

  return <>{children}</>;
}
