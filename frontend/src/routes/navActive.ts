import { matchPath } from 'react-router-dom';
import { paths } from '../routes/paths';

/** Detalle de reserva en flujo dueño (desde reservas de mis autos). */
export function isOwnerReservationDetailPath(pathname: string, search: string): boolean {
  if (!/^\/my-reservations\/[^/]+/.test(pathname)) return false;
  const params = new URLSearchParams(search);
  if (params.get('role')?.toUpperCase() === 'OWNER') return true;
  return params.has('fromCar');
}

/** Nav principal "Mis reservas": lista rider o detalle sin contexto dueño. */
export function isRiderReservationsNavActive(pathname: string, search: string): boolean {
  if (pathname === paths.myReservations) return true;
  if (!/^\/my-reservations\/[^/]+/.test(pathname)) return false;
  return !isOwnerReservationDetailPath(pathname, search);
}

/** Dropdown "Reservas de mis autos". */
export function isOwnerReservationsNavActive(pathname: string, search: string): boolean {
  if (
    matchPath({ path: paths.ownerReservations, end: true }, pathname) ||
    matchPath({ path: `${paths.ownerReservations}/:carId`, end: true }, pathname)
  ) {
    return true;
  }
  return isOwnerReservationDetailPath(pathname, search);
}

/** Dropdown "Mis autos" — solo flota / detalle de auto, no el flujo de reservas. */
export function isMyCarsNavActive(pathname: string, _search: string = ''): boolean {
  return (
    matchPath({ path: paths.myCars, end: true }, pathname) != null ||
    matchPath({ path: '/my-cars/car/:id', end: true }, pathname) != null
  );
}
