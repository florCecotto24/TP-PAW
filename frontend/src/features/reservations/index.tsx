// Punto de entrada público del área RESERVATIONS.
// El dueño del AppRouter monta `reservationsRoutes` (como children de Layout)
// y el dueño de src/i18n/* mergea `reservationsI18n` por locale.
import type { RouteObject } from 'react-router-dom';
import NewReservationPage from './pages/NewReservationPage';
import MyReservationsPage from './pages/MyReservationsPage';
import ReservationDetailPage from './pages/ReservationDetailPage';
import { reservationsI18n } from './i18n';

export const reservationsRoutes: RouteObject[] = [
  { path: 'reservar/:carId', element: <NewReservationPage /> },
  { path: 'mis-reservas', element: <MyReservationsPage scope="rider" /> },
  // Pantalla separada del owner (como el viejo /my-cars/reservations).
  // 'reservas' es estático → React Router lo prioriza sobre /mis-autos/:id.
  { path: 'mis-autos/reservas', element: <MyReservationsPage scope="owner" /> },
  { path: 'reservas/:id', element: <ReservationDetailPage /> },
];

export { reservationsI18n };
export * from './types';
