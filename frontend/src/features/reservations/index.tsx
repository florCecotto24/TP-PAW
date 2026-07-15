import { lazy } from 'react';
import type { RouteObject } from 'react-router-dom';
import { reservationsI18n } from './i18n';

// Ninguna es la ruta index de la app -> code-splitting seguro.
const NewReservationPage = lazy(() => import('./pages/NewReservationPage'));
const MyReservationsPage = lazy(() => import('./pages/MyReservationsPage'));
const ReservationDetailPage = lazy(() => import('./pages/ReservationDetailPage'));
const ReservationConfirmationPage = lazy(() => import('./pages/ReservationConfirmationPage'));

export const reservationsRoutes: RouteObject[] = [
  { path: 'cars/:carId/reservation/new', element: <NewReservationPage /> },
  { path: 'my-reservations', element: <MyReservationsPage scope="rider" /> },
  { path: 'my-cars/reservations', element: <MyReservationsPage scope="owner" /> },
  { path: 'my-cars/reservations/:carId', element: <MyReservationsPage scope="owner" /> },
  { path: 'my-reservations/:id/confirmation', element: <ReservationConfirmationPage /> },
  { path: 'my-reservations/:id', element: <ReservationDetailPage /> },
];

export { reservationsI18n };
export * from './types';
