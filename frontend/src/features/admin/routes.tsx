import type { RouteObject } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';
import AdminPanelPage from './pages/AdminPanelPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminCatalogPage from './pages/AdminCatalogPage';
import AdminCarsPage from './pages/AdminCarsPage';
import AdminReservationsPage from './pages/AdminReservationsPage';
import AdminReservationChatPage from './pages/AdminReservationChatPage';

// Rutas del área ADMIN. Pensadas para anidarse bajo `/admin` en el AppRouter
// del host (el host monta `adminRoutes` como children de un `path: 'admin'`).
// Por eso los paths son RELATIVOS a `/admin`. `AdminLayout` gatea por rol y
// pinta la navegación + el <Outlet> de la sub-ruta activa.
//
// Mapeo SPA (ES) -> API:
//   /admin            -> AdminPanelPage (índice)
//   /admin/usuarios   -> GET /users        + PATCH /users/{id}
//   /admin/catalogo   -> GET /brands?validated=false (+ models) + PATCH/DELETE
//   /admin/autos      -> GET /cars?status=...        + PATCH /cars/{id}
//   /admin/reservas   -> GET /reservations           (read-only)
//   /admin/reservas/:id/chat -> GET /reservations/{id}/messages (admin audit, read-only)
export const adminRoutes: RouteObject[] = [
  {
    path: 'admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <AdminPanelPage /> },
      { path: 'usuarios', element: <AdminUsersPage /> },
      { path: 'catalogo', element: <AdminCatalogPage /> },
      { path: 'autos', element: <AdminCarsPage /> },
      { path: 'reservas', element: <AdminReservationsPage /> },
      { path: 'reservas/:id/chat', element: <AdminReservationChatPage /> },
    ],
  },
];
