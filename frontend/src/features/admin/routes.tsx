import { lazy } from 'react';
import type { RouteObject } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';

// Code-splitting: el área admin es solo para rol ADMIN, así que sus páginas se
// cargan bajo demanda (no inflan el bundle inicial de visitantes anónimos /
// usuarios comunes). `AdminLayout` (shell + guard + nav) queda eager porque es
// el punto de entrada de `/admin`; el `<Suspense>` que cubre estos lazy() vive
// en su `<Outlet>`.
const AdminPanelPage = lazy(() => import('./pages/AdminPanelPage'));
const AdminUsersPage = lazy(() => import('./pages/AdminUsersPage'));
const AdminCreateUserPage = lazy(() => import('./pages/AdminCreateUserPage'));
const AdminCatalogPage = lazy(() => import('./pages/AdminCatalogPage'));
const AdminCarsPage = lazy(() => import('./pages/AdminCarsPage'));
const AdminReservationsPage = lazy(() => import('./pages/AdminReservationsPage'));
const AdminReservationChatPage = lazy(() => import('./pages/AdminReservationChatPage'));

export const adminRoutes: RouteObject[] = [
  {
    path: 'admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminPanelPage /> },
      { path: 'panel', element: <AdminPanelPage /> },
      { path: 'users', element: <AdminUsersPage /> },
      { path: 'users/create', element: <AdminCreateUserPage /> },
      { path: 'catalog', element: <AdminCatalogPage /> },
      { path: 'cars', element: <AdminCarsPage /> },
      { path: 'reservations', element: <AdminReservationsPage /> },
      { path: 'reservations/:id/chat', element: <AdminReservationChatPage /> },
    ],
  },
];
