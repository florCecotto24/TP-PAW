import type { RouteObject } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';
import AdminPanelPage from './pages/AdminPanelPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminCatalogPage from './pages/AdminCatalogPage';
import AdminCarsPage from './pages/AdminCarsPage';
import AdminReservationsPage from './pages/AdminReservationsPage';
import AdminReservationChatPage from './pages/AdminReservationChatPage';

export const adminRoutes: RouteObject[] = [
  {
    path: 'admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminPanelPage /> },
      { path: 'panel', element: <AdminPanelPage /> },
      { path: 'users', element: <AdminUsersPage /> },
      { path: 'catalog', element: <AdminCatalogPage /> },
      { path: 'cars', element: <AdminCarsPage /> },
      { path: 'reservations', element: <AdminReservationsPage /> },
      { path: 'reservations/:id/chat', element: <AdminReservationChatPage /> },
    ],
  },
];
