/**
 * Canonical API item URNs for bookmark/F5 when {@code location.state} has no hypermedia self.
 * List → detail navigation must pass {@code links.self} in router state; these helpers align
 * with OpenAPI paths and {@link getCollectionPath} from API discovery.
 */
import { getCollectionPath, type ApiCollectionName } from './apiDiscovery';

/** Item URN under a collection (e.g. {@code /cars/42}). */
export function canonicalItemUri(collection: ApiCollectionName, id: string): string {
  const base = getCollectionPath(collection).replace(/\/+$/, '');
  return `${base}/${id}`;
}

export function canonicalCarUri(carId: string): string {
  return canonicalItemUri('cars', carId);
}

export function canonicalReservationUri(reservationId: string): string {
  return canonicalItemUri('reservations', reservationId);
}

export function canonicalUserUri(userId: string): string {
  return canonicalItemUri('users', userId);
}

export interface ResolveResourceUriOptions {
  /** Preferred: {@code links.self} from list navigation. */
  stateUri?: string | null;
  /** Route {@code :id} for bookmark/deep-link fallback. */
  routeId?: string | null;
  collection: ApiCollectionName;
}

/** Prefer hypermedia self from navigation; otherwise canonical item URI from route id. */
export function resolveResourceUri(opts: ResolveResourceUriOptions): string | null {
  const fromState = opts.stateUri?.trim();
  if (fromState) return fromState;
  const routeId = opts.routeId?.trim();
  if (routeId) return canonicalItemUri(opts.collection, routeId);
  return null;
}
