/**
 * Client-side routes aligned with the legacy JSP URL shape (English paths).
 */
export const paths = {
  home: '/',
  search: '/search',
  login: '/login',
  register: '/register',
  verifyEmail: '/verify-email',
  forgotPassword: '/forgot-password',
  publishCar: '/publish-car',
  myCars: '/my-cars',
  ownerReservations: '/my-cars/reservations',
  myReservations: '/my-reservations',
  myFavorites: '/my-favorites',
  profile: '/profile',
  admin: {
    root: '/admin',
    panel: '/admin/panel',
    users: '/admin/users',
    catalog: '/admin/catalog',
    cars: '/admin/cars',
    reservations: '/admin/reservations',
    createAdmin: '/admin/users/create',
  },
} as const;

export function carDetail(id: string | number, query?: Record<string, string>): string {
  const base = `/cars/${id}`;
  if (!query || Object.keys(query).length === 0) return base;
  return `${base}?${new URLSearchParams(query)}`;
}

export function myCarDetail(id: string | number): string {
  return `/my-cars/car/${id}`;
}

export function ownerReservationsCar(carId: string | number): string {
  return `/my-cars/reservations/${carId}`;
}

export function newReservation(carId: string | number): string {
  return `/cars/${carId}/reservation/new`;
}

export function myReservationDetail(id: string | number, query?: Record<string, string>): string {
  const base = `/my-reservations/${id}`;
  if (!query || Object.keys(query).length === 0) return base;
  return `${base}?${new URLSearchParams(query)}`;
}

export function reservationConfirmation(id: string | number): string {
  return `/my-reservations/${id}/confirmation`;
}

export function publicProfile(userId: string | number): string {
  return `/users/${userId}/profile`;
}

export function adminReservationChat(id: string | number): string {
  return `/admin/reservations/${id}/chat`;
}

export function withQuery(basePath: string, params: URLSearchParams): string {
  const qs = params.toString();
  return qs ? `${basePath}?${qs}` : basePath;
}
