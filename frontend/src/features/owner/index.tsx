import { lazy } from 'react';
import type { RouteObject } from 'react-router-dom';

// Ninguna de estas es la ruta index de la app (esa es browse Home, que se queda
// eager) -> code-splitting seguro: solo se llega acá navegando.
const PublishCarPage = lazy(() => import('./PublishCarPage'));
const MyCarsPage = lazy(() => import('./MyCarsPage'));
const OwnerCarDetailPage = lazy(() => import('./OwnerCarDetailPage'));

export const ownerRoutes: RouteObject[] = [
  { path: 'publish-car', element: <PublishCarPage /> },
  { path: 'my-cars', element: <MyCarsPage /> },
  { path: 'my-cars/car/:id', element: <OwnerCarDetailPage /> },
];

export { ownerI18n } from './i18n';
export type * from './types';
