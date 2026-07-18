/**
 * Canonical API item URNs for bookmark/F5 when {@code location.state} has no hypermedia self.
 * List → detail navigation must pass {@code links.self} in router state; deep links may also
 * carry {@code ?self=} (relative API path). Route-id recovery expands discovery item templates.
 */
import { expandItemTemplate, type ApiCollectionName } from './apiDiscovery';
import { hrefToRelativeApiPath } from './uri';

/** Item URI expanded from the API index descriptor (e.g. {@code /cars/42}). */
export function canonicalItemUri(collection: ApiCollectionName, id: string): string {
  return expandItemTemplate(collection, id);
}

export function canonicalCarUri(carId: string): string {
  return canonicalItemUri('cars', carId);
}

/**
 * Accepts only a relative API item path for {@code collection} (optionally matching {@code routeId}).
 * Rejects open-redirect style values and paths that do not match the collection shape.
 */
export function sanitizeResourceSelfParam(
  raw: string | null | undefined,
  collection: ApiCollectionName,
  routeId?: string | null,
): string | null {
  if (raw == null) return null;
  const trimmed = raw.trim();
  if (!trimmed) return null;
  if (/^(javascript|data|vbscript):/i.test(trimmed)) return null;

  let relative: string;
  try {
    relative = hrefToRelativeApiPath(trimmed).split('?')[0].replace(/\/+$/, '') || '/';
  } catch {
    return null;
  }
  if (relative.includes('..')) return null;

  const match = relative.match(new RegExp(`^/${collection}/([^/]+)$`));
  if (!match) return null;
  const idSeg = match[1];
  if (routeId) {
    const expected = routeId.trim();
    if (idSeg !== expected && decodeURIComponent(idSeg) !== expected) return null;
  }
  return `/${collection}/${idSeg}`;
}

export interface ResolveResourceUriOptions {
  /** Preferred: {@code links.self} from list navigation ({@code location.state}). */
  stateUri?: string | null;
  /**
   * Deep-link / mail CTA: {@code ?self=} with a relative API path
   * (e.g. {@code /cars/42}). Validated against {@link collection} (+ optional route id).
   */
  querySelf?: string | null;
  /** Route {@code :id} for bookmark/deep-link fallback via itemTemplate. */
  routeId?: string | null;
  collection: ApiCollectionName;
}

/**
 * Prefer hypermedia self from navigation, then {@code ?self=}, then expand the
 * discovered item template with the route id.
 */
export function resolveResourceUri(opts: ResolveResourceUriOptions): string | null {
  const fromState = opts.stateUri?.trim();
  if (fromState) return fromState;

  const fromQuery = sanitizeResourceSelfParam(opts.querySelf, opts.collection, opts.routeId);
  if (fromQuery) return fromQuery;

  const routeId = opts.routeId?.trim();
  if (routeId) return canonicalItemUri(opts.collection, routeId);
  return null;
}
