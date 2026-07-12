import { carDetail } from './paths';

/** Optional React Router location state for hypermedia-backed navigation. */

export interface CarDetailLocationState {
  carSelf?: string;
}

export interface NewReservationLocationState {
  carSelf?: string;
}

export interface AdminReservationChatLocationState {
  messagesLink?: string;
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

export function isAppLinkTarget(value: unknown): value is AppLinkTarget {
  return (
    typeof value === 'object'
    && value !== null
    && 'pathname' in value
    && typeof (value as AppLinkTarget).pathname === 'string'
  );
}
