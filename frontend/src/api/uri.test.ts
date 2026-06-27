import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiAssetUrl, resolveApiUrl } from './uri';

describe('resolveApiUrl', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('testResolveApiUrlPrefixesRelativePathsWithBase', () => {
    // 1.Arrange
    vi.stubEnv('BASE_URL', '/webapp/');

    // 2.Act
    const resolved = resolveApiUrl('/cars?page=2');

    // 3.Assert
    expect(resolved).toBe('/webapp/cars?page=2');
  });

  it('testResolveApiUrlKeepsAbsoluteJerseyLinksUntouched', () => {
    // 1.Arrange
    vi.stubEnv('BASE_URL', '/webapp/');
    vi.stubGlobal('window', { location: { origin: 'http://localhost:8080' } });

    // 2.Act
    const resolved = resolveApiUrl('http://localhost:8080/webapp/cars?page=2');

    // 3.Assert
    expect(resolved).toBe('http://localhost:8080/webapp/cars?page=2');
  });

  it('testApiAssetUrlDoesNotDoublePrefixAbsolutePictureLinks', () => {
    // 1.Arrange
    vi.stubEnv('BASE_URL', '/webapp/');

    // 2.Act
    const src = apiAssetUrl('http://localhost:8080/webapp/cars/1/pictures/2');

    // 3.Assert
    expect(src).toBe('http://localhost:8080/webapp/cars/1/pictures/2');
  });
});
