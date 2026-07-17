import { describe, expect, it } from 'vitest';

import { API_COLLECTION_FALLBACK_PATHS } from './apiDiscovery';
import { canonicalCarUri, canonicalItemUri, resolveResourceUri } from './resourceUri';

describe('resourceUri', () => {
  it('testResolveResourceUriPrefersStateOverRouteId', () => {
    // 1.Arrange
    const stateSelf = '/cars/99';
    // 2.Act
    const resolved = resolveResourceUri({
      stateUri: stateSelf,
      routeId: '1',
      collection: 'cars',
    });
    // 3.Assert
    expect(resolved).toBe(stateSelf);
  });

  it('testResolveResourceUriFallsBackToCanonicalItemUri', () => {
    // 1.Arrange
    const id = '42';
    // 2.Act
    const resolved = resolveResourceUri({
      routeId: id,
      collection: 'reservations',
    });
    // 3.Assert
    expect(resolved).toBe(canonicalItemUri('reservations', id));
    expect(resolved).toBe(`${API_COLLECTION_FALLBACK_PATHS.reservations}/${id}`);
  });

  it('testCanonicalCarUriUsesDiscoveryCollectionPath', () => {
    // 2.Act / 3.Assert
    expect(canonicalCarUri('7')).toBe(`${API_COLLECTION_FALLBACK_PATHS.cars}/7`);
  });
});
