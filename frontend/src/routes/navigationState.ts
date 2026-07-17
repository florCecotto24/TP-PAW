import { carDetail, myCarDetail, myReservationDetail, publicProfile } from './paths';

/** Optional React Router location state for hypermedia-backed navigation. */

export interface CarDetailLocationState {
  carSelf?: string;
}

export interface OwnerCarDetailLocationState {
  carSelf?: string;
}

export interface NewReservationLocationState {
  carSelf?: string;
}

export interface ReservationDetailLocationState {
  reservationSelf?: string;
}

export interface ReservationConfirmationLocationState {
  reservationSelf?: string;
}

export interface PublicProfileLocationState {
  userSelf?: string;
}

export interface AdminReservationChatLocationState {
  messagesLink?: string;
  reservationSelf?: string;
}

/** Internal SPA link; {@code state} is passed separately to {@code Link} / {@code navigate}. */
export interface AppLinkTarget {
  pathname: string;
  state?: unknown;
}

/** SPA path to car detail; carries {@code links.self} when known so the target page can follow hypermedia. */
export function carDetailTo(
  carId: string | number,
  carSelf?: string | null,
  query?: Record<string, string>,
): AppLinkTarget {
  const pathname = carDetail(carId, query);
  if (!carSelf) return { pathname };
  return { pathname, state: { carSelf } satisfies CarDetailLocationState };
}

export function myCarDetailTo(
  carId: string | number,
  carSelf?: string | null,
): AppLinkTarget {
  const pathname = myCarDetail(carId);
  if (!carSelf) return { pathname };
  return { pathname, state: { carSelf } satisfies OwnerCarDetailLocationState };
}

export function myReservationDetailTo(
  reservationId: string | number,
  reservationSelf?: string | null,
  query?: Record<string, string>,
): AppLinkTarget {
  const pathname = myReservationDetail(reservationId, query);
  if (!reservationSelf) return { pathname };
  return {
    pathname,
    state: { reservationSelf } satisfies ReservationDetailLocationState,
  };
}

export function publicProfileTo(
  userId: string | number,
  userSelf?: string | null,
): AppLinkTarget {
  const pathname = publicProfile(userId);
  if (!userSelf) return { pathname };
  return { pathname, state: { userSelf } satisfies PublicProfileLocationState };
}

export function isAppLinkTarget(value: unknown): value is AppLinkTarget {
  return (
    typeof value === 'object'
    && value !== null
    && 'pathname' in value
    && typeof (value as AppLinkTarget).pathname === 'string'
  );
}
