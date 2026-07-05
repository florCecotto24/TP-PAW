import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiAssetUrl, collapseDuplicateSlashes, joinBaseAndPath, resolveApiUrl } from './uri';

describe('resolveApiUrl', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('testResolveApiUrlPrefixesRelativePathsWithBase', () => {
    vi.stubEnv('BASE_URL', '/webapp/');

    const resolved = resolveApiUrl('/cars?page=2');

    expect(resolved).toBe('/webapp/cars?page=2');
  });

  it('testResolveApiUrlKeepsAbsoluteJerseyLinksUntouched', () => {
    vi.stubEnv('BASE_URL', '/webapp/');
    vi.stubGlobal('window', { location: { origin: 'http://localhost:8080' } });

    const resolved = resolveApiUrl('http://localhost:8080/webapp/cars?page=2');

    expect(resolved).toBe('http://localhost:8080/webapp/cars?page=2');
  });

  it('testApiAssetUrlDoesNotDoublePrefixAbsolutePictureLinks', () => {
    vi.stubEnv('BASE_URL', '/webapp/');

    const src = apiAssetUrl('http://localhost:8080/webapp/cars/1/pictures/2');

    expect(src).toBe('http://localhost:8080/webapp/cars/1/pictures/2');
  });

  it('does not double-prefix paths that already include the context path', () => {
    vi.stubEnv('BASE_URL', '/webapp/');

    expect(resolveApiUrl('/webapp/reservations/1/messages')).toBe('/webapp/reservations/1/messages');
  });

  it('collapses duplicate slashes that trigger StrictHttpFirewall', () => {
    vi.stubEnv('BASE_URL', '/webapp/');

    expect(resolveApiUrl('//reservations/1/messages')).toBe('/webapp/reservations/1/messages');
  });

  it('fixes absolute same-origin links missing the context path', () => {
    vi.stubEnv('BASE_URL', '/webapp/');
    vi.stubGlobal('window', { location: { origin: 'http://localhost:8080' } });

    expect(resolveApiUrl('http://localhost:8080/reservations/1/messages')).toBe(
      'http://localhost:8080/webapp/reservations/1/messages',
    );
  });

  it('collapses duplicate slashes in absolute same-origin links', () => {
    vi.stubEnv('BASE_URL', '/webapp/');
    vi.stubGlobal('window', { location: { origin: 'http://localhost:8080' } });

    expect(resolveApiUrl('http://localhost:8080/webapp//reservations/1/messages')).toBe(
      'http://localhost:8080/webapp/reservations/1/messages',
    );
  });
});

describe('joinBaseAndPath', () => {
  it('joins base and relative api paths', () => {
    expect(joinBaseAndPath('/webapp', '/reservations/1/messages')).toBe('/webapp/reservations/1/messages');
  });

  it('collapses slashes', () => {
    expect(collapseDuplicateSlashes('/webapp//reservations/1')).toBe('/webapp/reservations/1');
  });
});
