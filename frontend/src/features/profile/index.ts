import { createElement, lazy } from 'react';
import type { RouteObject } from 'react-router-dom';
import { profileI18n } from './i18n';

// Ninguna de estas páginas es la ruta index de la app -> code-splitting seguro.
// No se re-exportan los componentes (no había consumidores externos reales): un
// re-export estático del barrel forzaría a bundlear el chunk igual, anulando el
// lazy() de las rutas de abajo.
const MyProfilePage = lazy(() => import('./MyProfilePage'));
const PublicProfilePage = lazy(() => import('./PublicProfilePage'));
const FavoritesPage = lazy(() => import('./FavoritesPage'));

export const profileRoutes: RouteObject[] = [
  { path: 'profile', element: createElement(MyProfilePage) },
  { path: 'users/:id/profile', element: createElement(PublicProfilePage) },
  { path: 'my-favorites', element: createElement(FavoritesPage) },
];

export { profileI18n };
export * from './types';
