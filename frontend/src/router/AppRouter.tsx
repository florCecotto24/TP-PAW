import { createBrowserRouter, RouterProvider, type RouteObject } from 'react-router-dom';
import { appBasePath } from '../appBasePath';
import Layout from '../components/Layout';
import NotFoundPage from '../pages/NotFoundPage';
import RequireAuth from '../components/RequireAuth';
import { authRoutes } from '../features/auth';
import { browseRoutes } from '../features/browse';
import { ownerRoutes } from '../features/owner';
import { reservationsRoutes } from '../features/reservations';
import { profileRoutes } from '../features/profile';
import { adminRoutes } from '../features/admin';

function protect(routes: RouteObject[], admin = false): RouteObject[] {
  return routes.map((r) => ({ ...r, element: <RequireAuth admin={admin}>{r.element}</RequireAuth> }));
}

const profileChildren: RouteObject[] = profileRoutes.map((r) =>
  r.path === 'users/:id/profile' ? r : { ...r, element: <RequireAuth>{r.element}</RequireAuth> },
);

const featureChildren: RouteObject[] = [
  ...browseRoutes,
  ...authRoutes,
  ...protect(ownerRoutes),
  ...protect(reservationsRoutes),
  ...profileChildren,
  ...protect(adminRoutes, true),
  { path: '*', element: <NotFoundPage /> },
];

const router = createBrowserRouter(
  [
    {
      path: '/',
      element: <Layout />,
      children: featureChildren,
    },
  ],
  { basename: appBasePath() || undefined },
);

export default function AppRouter() {
  return <RouterProvider router={router} />;
}
