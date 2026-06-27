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

// Rutas de la SPA EN ESPAÑOL para no colisionar con los sustantivos en inglés
// de la API (LINEAMIENTOS §3.3). Deep-link refresh lo sirve index.html.
// Las rutas protegidas se envuelven en RequireAuth (réplica de Spring Security
// del proyecto original: sin sesión → redirección al login).

function protect(routes: RouteObject[], admin = false): RouteObject[] {
  return routes.map((r) => ({ ...r, element: <RequireAuth admin={admin}>{r.element}</RequireAuth> }));
}

// Perfil propio y favoritos requieren sesión; el perfil público (usuarios/:id) no.
const profileChildren: RouteObject[] = profileRoutes.map((r) =>
  r.path === 'usuarios/:id' ? r : { ...r, element: <RequireAuth>{r.element}</RequireAuth> },
);

const featureChildren: RouteObject[] = [
  ...browseRoutes, // públicas: /, /buscar, /autos/:id
  ...authRoutes, // públicas: /ingresar, /registrarse, /verificar-email, /recuperar-clave
  ...protect(ownerRoutes), // /publicar, /mis-autos*
  ...protect(reservationsRoutes), // /reservar/:carId, /mis-reservas, /reservas/:id
  ...profileChildren, // /perfil (auth), /favoritos (auth), /usuarios/:id (público)
  ...protect(adminRoutes, true), // /admin/* (rol ADMIN)
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
