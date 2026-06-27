// Punto de entrada del área OWNER (publicar auto + mis autos + disponibilidad).
// Exporta las rutas (a montar en AppRouter) y el bundle i18n (a mergear en el
// i18n global). El resto de la app NO importa los componentes directamente.

import type { RouteObject } from 'react-router-dom';
import PublishCarPage from './PublishCarPage';
import MyCarsPage from './MyCarsPage';
import OwnerCarDetailPage from './OwnerCarDetailPage';

export const ownerRoutes: RouteObject[] = [
  { path: 'publicar', element: <PublishCarPage /> },
  { path: 'mis-autos', element: <MyCarsPage /> },
  { path: 'mis-autos/:id', element: <OwnerCarDetailPage /> },
];

export { ownerI18n } from './i18n';
export type * from './types';
