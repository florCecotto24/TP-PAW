/**
 * Canonical API item URNs for bookmark/F5 when {@code location.state} has no hypermedia self.
 * List → detail navigation must pass {@code links.self} in router state; these helpers align
 * with URI templates published by API discovery.
 */
import { expandItemTemplate, type ApiCollectionName } from './apiDiscovery';

/** Item URI expanded from the API index descriptor (e.g. {@code /cars/42}). */
export function canonicalItemUri(collection: ApiCollectionName, id: string): string {
  return expandItemTemplate(collection, id);
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

/** Prefer hypermedia self from navigation; otherwise expand the discovered template. */
export function resolveResourceUri(opts: ResolveResourceUriOptions): string | null {
  const fromState = opts.stateUri?.trim();
  if (fromState) return fromState;
  const routeId = opts.routeId?.trim();
  if (routeId) return canonicalItemUri(opts.collection, routeId);
  return null;
}
