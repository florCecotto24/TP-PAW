import type { RouteObject } from 'react-router-dom';
import NewReservationPage from './pages/NewReservationPage';
import MyReservationsPage from './pages/MyReservationsPage';
import ReservationDetailPage from './pages/ReservationDetailPage';
import { reservationsI18n } from './i18n';

export const reservationsRoutes: RouteObject[] = [
  { path: 'cars/:carId/reservation/new', element: <NewReservationPage /> },
  { path: 'my-reservations', element: <MyReservationsPage scope="rider" /> },
  { path: 'my-cars/reservations', element: <MyReservationsPage scope="owner" /> },
  { path: 'my-cars/reservations/:carId', element: <MyReservationsPage scope="owner" /> },
  { path: 'my-reservations/:id', element: <ReservationDetailPage /> },
];

export { reservationsI18n };
export * from './types';
