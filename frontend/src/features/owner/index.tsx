import type { RouteObject } from 'react-router-dom';
import PublishCarPage from './PublishCarPage';
import MyCarsPage from './MyCarsPage';
import OwnerCarDetailPage from './OwnerCarDetailPage';

export const ownerRoutes: RouteObject[] = [
  { path: 'publish-car', element: <PublishCarPage /> },
  { path: 'my-cars', element: <MyCarsPage /> },
  { path: 'my-cars/car/:id', element: <OwnerCarDetailPage /> },
];

export { ownerI18n } from './i18n';
export type * from './types';
