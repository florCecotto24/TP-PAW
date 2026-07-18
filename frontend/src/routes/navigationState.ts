import {
  adminReservationChat,
  carDetail,
  myCarDetail,
  myReservationDetail,
  publicProfile,
  reservationConfirmation,
} from './paths';
import { hrefToRelativeApiPath } from '../api/uri';

/** Optional React Router location state for hypermedia-backed navigation. */

export interface CarDetailLocationState {
  carSelf?: string;
}

export interface OwnerCarDetailLocationState {
  carSelf?: string;
}

export interface NewReservationLocationState {
  carSelf?: string;
  /** Canonical availability URN from bookable-segment links.availability. */
  availabilityUri?: string;
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

/** Relative API path for {@code ?self=} (survives F5; validated by {@code resolveResourceUri}). */
export function selfQueryValue(apiSelfUri: string): string {
  return hrefToRelativeApiPath(apiSelfUri);
}

function mergeSelfQuery(
  query: Record<string, string> | undefined,
  apiSelfUri: string | null | undefined,
): Record<string, string> | undefined {
  if (!apiSelfUri) return query;
  return { ...query, self: selfQueryValue(apiSelfUri) };
}

/** SPA path to car detail; carries {@code links.self} in state and {@code ?self=}. */
export function carDetailTo(
  carId: string | number,
  carSelf?: string | null,
  query?: Record<string, string>,
): AppLinkTarget {
  const pathname = carDetail(carId, mergeSelfQuery(query, carSelf));
  if (!carSelf) return { pathname };
  return { pathname, state: { carSelf } satisfies CarDetailLocationState };
}

export function myCarDetailTo(
  carId: string | number,
  carSelf?: string | null,
): AppLinkTarget {
  const pathname = myCarDetail(carId, mergeSelfQuery(undefined, carSelf));
  if (!carSelf) return { pathname };
  return { pathname, state: { carSelf } satisfies OwnerCarDetailLocationState };
}

export function myReservationDetailTo(
  reservationId: string | number,
  reservationSelf?: string | null,
  query?: Record<string, string>,
): AppLinkTarget {
  const pathname = myReservationDetail(reservationId, mergeSelfQuery(query, reservationSelf));
  if (!reservationSelf) return { pathname };
  return {
    pathname,
    state: { reservationSelf } satisfies ReservationDetailLocationState,
  };
}

export function reservationConfirmationTo(
  reservationId: string | number,
  reservationSelf?: string | null,
): AppLinkTarget {
  const pathname = reservationConfirmation(reservationId, mergeSelfQuery(undefined, reservationSelf));
  if (!reservationSelf) return { pathname };
  return {
    pathname,
    state: { reservationSelf } satisfies ReservationConfirmationLocationState,
  };
}

export function publicProfileTo(
  userId: string | number,
  userSelf?: string | null,
  query?: Record<string, string>,
): AppLinkTarget {
  const pathname = publicProfile(userId, mergeSelfQuery(query, userSelf));
  if (!userSelf) return { pathname };
  return { pathname, state: { userSelf } satisfies PublicProfileLocationState };
}

export function adminReservationChatTo(
  reservationId: string | number,
  reservationSelf?: string | null,
  messagesLink?: string | null,
): AppLinkTarget {
  const pathname = adminReservationChat(reservationId, mergeSelfQuery(undefined, reservationSelf));
  if (!reservationSelf && !messagesLink) return { pathname };
  return {
    pathname,
    state: {
      ...(reservationSelf ? { reservationSelf } : {}),
      ...(messagesLink ? { messagesLink } : {}),
    } satisfies AdminReservationChatLocationState,
  };
}

export function isAppLinkTarget(value: unknown): value is AppLinkTarget {
  return (
    typeof value === 'object'
    && value !== null
    && 'pathname' in value
    && typeof (value as AppLinkTarget).pathname === 'string'
  );
}
