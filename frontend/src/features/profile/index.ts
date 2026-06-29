import { createElement } from 'react';
import type { RouteObject } from 'react-router-dom';
import MyProfilePage from './MyProfilePage';
import PublicProfilePage from './PublicProfilePage';
import FavoritesPage from './FavoritesPage';
import { profileI18n } from './i18n';

export const profileRoutes: RouteObject[] = [
  { path: 'profile', element: createElement(MyProfilePage) },
  { path: 'users/:id/profile', element: createElement(PublicProfilePage) },
  { path: 'my-favorites', element: createElement(FavoritesPage) },
];

export { profileI18n };
export { default as MyProfilePage } from './MyProfilePage';
export { default as PublicProfilePage } from './PublicProfilePage';
export { default as FavoritesPage } from './FavoritesPage';
export * from './types';
