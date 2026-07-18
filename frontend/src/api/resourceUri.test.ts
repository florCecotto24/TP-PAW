import { describe, expect, it } from 'vitest';

import { useApiDiscoveryStore } from './apiDiscovery';
import {
  canonicalCarUri,
  canonicalItemUri,
  resolveResourceUri,
  sanitizeResourceSelfParam,
} from './resourceUri';

describe('resourceUri', () => {
  useApiDiscoveryStore.setState({
    index: {
      links: {},
      resources: {
        cars: { href: '/cars', itemTemplate: '/cars/{id}' },
        reservations: { href: '/reservations', itemTemplate: '/reservations/{id}' },
      },
    },
    ready: true,
  });

  it('testResolveResourceUriPrefersStateOverRouteId', () => {
    // 1.Arrange
    const stateSelf = '/cars/99';
    // 2.Act
    const resolved = resolveResourceUri({
      stateUri: stateSelf,
      querySelf: '/cars/1',
      routeId: '1',
      collection: 'cars',
    });
    // 3.Assert
    expect(resolved).toBe(stateSelf);
  });

  it('testResolveResourceUriPrefersQuerySelfOverItemTemplate', () => {
    // 1.Arrange / 2.Act
    const resolved = resolveResourceUri({
      querySelf: '/cars/42',
      routeId: '42',
      collection: 'cars',
    });
    // 3.Assert
    expect(resolved).toBe('/cars/42');
  });

  it('testResolveResourceUriExpandsDiscoveredItemTemplate', () => {
    // 1.Arrange
    const id = '42';
    // 2.Act
    const resolved = resolveResourceUri({
      routeId: id,
      collection: 'reservations',
    });
    // 3.Assert
    expect(resolved).toBe(canonicalItemUri('reservations', id));
    expect(resolved).toBe('/reservations/42');
  });

  it('testSanitizeResourceSelfParamRejectsWrongCollectionOrId', () => {
    // 2.Act / 3.Assert
    expect(sanitizeResourceSelfParam('/users/1', 'cars', '1')).toBeNull();
    expect(sanitizeResourceSelfParam('/cars/99', 'cars', '1')).toBeNull();
    expect(sanitizeResourceSelfParam('javascript:alert(1)', 'cars', '1')).toBeNull();
    expect(sanitizeResourceSelfParam('/cars/7', 'cars', '7')).toBe('/cars/7');
  });

  it('testCanonicalCarUriEncodesTemplateIdentifier', () => {
    // 2.Act / 3.Assert
    expect(canonicalCarUri('7/a')).toBe('/cars/7%2Fa');
  });

  it('testCanonicalItemUriFailsWithoutPublishedTemplate', () => {
    // 1.Arrange
    useApiDiscoveryStore.setState({ index: null });
    // 2.Act / 3.Assert
    expect(() => canonicalItemUri('cars', '7')).toThrow('missingItemTemplate');
  });
});
