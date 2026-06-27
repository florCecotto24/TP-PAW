import { createElement } from 'react';
import type { RouteObject } from 'react-router-dom';
import MyProfilePage from './MyProfilePage';
import PublicProfilePage from './PublicProfilePage';
import FavoritesPage from './FavoritesPage';
import { profileI18n } from './i18n';

// =============================================================================
// Barrel del área PROFILE. El integrador monta `profileRoutes` como children del
// layout en AppRouter y mergea `profileI18n` en los recursos i18n globales.
// Rutas SPA en español (LINEAMIENTOS §3.3); el mapeo a la API es explícito en
// la capa de cliente/feature.
//
// Nota: este archivo es .ts (no .tsx) por contrato, así que los elementos de
// ruta se construyen con createElement en vez de JSX.
// =============================================================================

export const profileRoutes: RouteObject[] = [
  { path: 'perfil', element: createElement(MyProfilePage) },
  { path: 'usuarios/:id', element: createElement(PublicProfilePage) },
  { path: 'favoritos', element: createElement(FavoritesPage) },
];

export { profileI18n };
export { default as MyProfilePage } from './MyProfilePage';
export { default as PublicProfilePage } from './PublicProfilePage';
export { default as FavoritesPage } from './FavoritesPage';
export * from './types';
