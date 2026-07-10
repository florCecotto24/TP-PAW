import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { devOriginUrl, stubDevWindow } from '../test/devOrigin';
import {
  HypermediaClient,
  parseLinkHeader,
  extractPageLinks,
  parseContentDispositionFileName,
  type TokenAccessors,
} from './client';
import {
  apiAssetUrl,
  apiBasePath,
  API_BASE,
  carCoverAssetUrl,
  canonicalApiUserPath,
  collapseDuplicateSlashes,
  joinBaseAndPath,
  profilePictureAssetUrlFromSelf,
  resolveApiUrl,
  resolveProfilePictureAssetUrl,
} from './uri';

function makeResponse(
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): Response {
  const h = new Headers(headers);
  const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body);
  return new Response(status === 204 ? null : text, { status, headers: h });
}

describe('hypermedia contract (frontend)', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  describe('Link header pagination (openapi)', () => {
    it('testParseLinkHeaderReadsNextAndLastRels', () => {
      // 1.Arrange
      const header =
        '</cars?page=2&category=suv>; rel="next", </cars?page=5&category=suv>; rel="last"';

      // 2.Act
      const rels = parseLinkHeader(header);

      // 3.Assert
      expect(rels.next).toBe('/cars?page=2&category=suv');
      expect(rels.last).toBe('/cars?page=5&category=suv');
    });

    it('testParseLinkHeaderReadsAllPaginationRels', () => {
      // 1.Arrange
      const header =
        '</cars?page=1>; rel="first", </cars?page=2>; rel="prev", </cars?page=4>; rel="next", </cars?page=9>; rel="last"';

      // 2.Act
      const rels = parseLinkHeader(header);

      // 3.Assert
      expect(rels).toMatchObject({
        first: '/cars?page=1',
        prev: '/cars?page=2',
        next: '/cars?page=4',
        last: '/cars?page=9',
      });
    });

    it('testParseLinkHeaderReadsAuthenticatedUserRel', () => {
      // 1.Arrange
      const header = '</users/42>; rel=authenticated-user';

      // 2.Act
      const rels = parseLinkHeader(header);

      // 3.Assert
      expect(rels['authenticated-user']).toBe('/users/42');
    });

    it('testParseLinkHeaderReturnsEmptyForNullOrBlank', () => {
      // 2.Act / 3.Assert
      expect(parseLinkHeader(null)).toEqual({});
      expect(parseLinkHeader('')).toEqual({});
    });

    it('testExtractPageLinksCombinesLinkWithXTotalCount', () => {
      // 1.Arrange
      const headers = new Headers({
        Link: '</cars?page=2>; rel="next", </cars?page=8>; rel="last"',
        'X-Total-Count': '94',
      });

      // 2.Act
      const page = extractPageLinks(headers);

      // 3.Assert
      expect(page.next).toBe('/cars?page=2');
      expect(page.last).toBe('/cars?page=8');
      expect(page.total).toBe(94);
    });
  });

  describe('URI resolution under /webapp/api (openapi links)', () => {
    it('testResolveApiUrlPrefixesRelativePathsWithApiMount', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const resolved = resolveApiUrl('/cars?page=2');

      // 3.Assert
      expect(resolved).toBe('/webapp/api/cars?page=2');
    });

    it('testResolveApiUrlDoesNotDuplicateApiSegment', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const apiIndex = resolveApiUrl('/api');
      const base = apiBasePath();

      // 3.Assert
      expect(apiIndex).toBe('/webapp/api');
      expect(base).toBe('/webapp/api');
    });

    it('testResolveApiUrlKeepsAbsoluteJerseyLinksUntouched', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      stubDevWindow();
      const link = devOriginUrl('/webapp/api/cars?page=2');

      // 2.Act
      const resolved = resolveApiUrl(link);

      // 3.Assert
      expect(resolved).toBe(link);
    });

    it('testApiAssetUrlDoesNotDoublePrefixAbsolutePictureLinks', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      const picture = devOriginUrl('/webapp/api/cars/1/pictures/2');

      // 2.Act
      const src = apiAssetUrl(picture);

      // 3.Assert
      expect(src).toBe(picture);
    });

    it('testResolveApiUrlKeepsContextPrefixedApiPaths', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const resolved = resolveApiUrl('/webapp/api/reservations/1/messages');

      // 3.Assert
      expect(resolved).toBe('/webapp/api/reservations/1/messages');
    });

    it('testResolveApiUrlInsertsApiSegmentForLegacyContextPaths', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const resolved = resolveApiUrl('/webapp/reservations/1/messages');

      // 3.Assert
      expect(resolved).toBe('/webapp/api/reservations/1/messages');
    });

    it('testResolveApiUrlCollapsesDuplicateSlashes', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const resolved = resolveApiUrl('//reservations/1/messages');

      // 3.Assert
      expect(resolved).toBe('/webapp/api/reservations/1/messages');
    });

    it('testResolveApiUrlFixesSameOriginLinksMissingContextPath', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      stubDevWindow();

      // 2.Act
      const resolved = resolveApiUrl(devOriginUrl('/reservations/1/messages'));

      // 3.Assert
      expect(resolved).toBe(devOriginUrl('/webapp/api/reservations/1/messages'));
    });

    it('testResolveApiUrlCollapsesSlashesInSameOriginAbsoluteLinks', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      stubDevWindow();

      // 2.Act
      const resolved = resolveApiUrl(devOriginUrl('/webapp//api/reservations/1/messages'));

      // 3.Assert
      expect(resolved).toBe(devOriginUrl('/webapp/api/reservations/1/messages'));
    });

    it('testJoinBaseAndPathMergesContextWithApiPath', () => {
      // 1.Arrange
      const base = '/webapp';
      const path = '/api/reservations/1/messages';

      // 2.Act
      const joined = joinBaseAndPath(base, path);

      // 3.Assert
      expect(joined).toBe('/webapp/api/reservations/1/messages');
    });

    it('testCollapseDuplicateSlashesNormalizesPath', () => {
      // 2.Act / 3.Assert
      expect(collapseDuplicateSlashes('/webapp//api/reservations/1')).toBe(
        '/webapp/api/reservations/1',
      );
    });

    it('testResolveProfilePictureAssetUrlUsesLinksProfilePicture', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const url = resolveProfilePictureAssetUrl({
        self: '/users/5',
        profilePicture: '/users/5/profile-picture',
      });

      // 3.Assert
      expect(url).toBe('/webapp/api/users/5/profile-picture');
    });

    it('testProfilePictureAssetUrlFromSelfMatchesSubResourceShape', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const url = profilePictureAssetUrlFromSelf('/users/9');

      // 3.Assert
      expect(url).toBe('/webapp/api/users/9/profile-picture');
    });

    it('testCarCoverAssetUrlPrefersLinksCover', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const url = carCoverAssetUrl('/cars/3', '/cars/3/pictures/primary');

      // 3.Assert
      expect(url).toBe('/webapp/api/cars/3/pictures/primary');
    });

    it('testCarCoverAssetUrlFallsBackToPrimaryPictureEndpoint', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const url = carCoverAssetUrl('/cars/3');

      // 3.Assert
      expect(url).toBe('/webapp/api/cars/3/pictures/primary');
    });

    it('testCanonicalApiUserPathNormalizesAuthenticatedUserLink', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      stubDevWindow();

      // 2.Act
      const path = canonicalApiUserPath(devOriginUrl('/webapp/users/42'));

      // 3.Assert
      expect(path).toBe('/users/42');
    });

    it('testApiBasePathJoinsContextAndApiMount', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');

      // 2.Act
      const base = apiBasePath();

      // 3.Assert
      expect(base).toBe('/webapp/api');
    });

    it('testApiBasePathWithoutWarContext', () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/');

      // 2.Act
      const base = apiBasePath();

      // 3.Assert
      expect(base).toBe(API_BASE);
    });
  });

  describe('auth and upload headers (openapi)', () => {
    let accessors: TokenAccessors;
    let client: HypermediaClient;
    const fetchMock = vi.fn();

    beforeEach(() => {
      accessors = {
        getAccessToken: () => 'access',
        getRefreshToken: () => 'refresh',
        onTokens: vi.fn(),
      };
      client = new HypermediaClient(accessors);
      vi.stubGlobal('fetch', fetchMock);
      fetchMock.mockReset();
    });

    it('testLoginAbsorbsAuthenticatedUserLinkAndTokenHeaders', async () => {
      // 1.Arrange
      const onTokens = vi.fn();
      const onAuthenticatedUser = vi.fn();
      const c = new HypermediaClient({ ...accessors, onTokens, onAuthenticatedUser });
      fetchMock.mockResolvedValueOnce(
        makeResponse(
          200,
          { resources: {} },
          {
            'X-Access-Token': 'a1',
            'X-Refresh-Token': 'r1',
            Link: '</users/42>; rel="authenticated-user"',
          },
        ),
      );

      // 2.Act
      await c.loginBasic('a@b.com', 'secret');

      // 3.Assert
      const init = fetchMock.mock.calls[0][1] as RequestInit;
      expect((init.headers as Headers).get('Authorization')).toMatch(/^Basic /);
      expect(onTokens).toHaveBeenCalledWith('a1', 'r1');
      expect(onAuthenticatedUser).toHaveBeenCalledWith('/users/42');
    });

    it('testFollowRewritesAuthenticatedUserLinksMissingApiSegment', async () => {
      // 1.Arrange
      vi.stubEnv('BASE_URL', '/webapp/');
      stubDevWindow();
      fetchMock.mockResolvedValueOnce(makeResponse(200, { forename: 'Ana' }));

      // 2.Act
      await client.follow(devOriginUrl('/webapp/users/42'), {
        accept: 'application/vnd.paw.user.private.v1+json',
      });

      // 3.Assert
      expect(fetchMock.mock.calls[0][0]).toBe(devOriginUrl('/webapp/api/users/42'));
    });

    it('testFollowUsesAbsolutePaginationLinksWithoutDoublePrefix', async () => {
      // 1.Arrange
      fetchMock.mockResolvedValueOnce(
        makeResponse(200, [{ links: { self: '/cars/1' } }], {
          Link: `<${devOriginUrl('/webapp/api/cars?page=2')}>; rel="next"`,
        }),
      );

      // 2.Act
      await client.follow(devOriginUrl('/webapp/api/cars?page=2'), {
        accept: 'application/vnd.paw.car.v1+json',
      });

      // 3.Assert
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock.mock.calls[0][0]).toBe(devOriginUrl('/webapp/api/cars?page=2'));
    });

    it('testPostFormDataOmitsContentTypeSoBrowserCanSetBoundary', async () => {
      // 1.Arrange
      fetchMock.mockResolvedValueOnce(makeResponse(201, { plate: 'ABC123' }));
      const fd = new FormData();
      fd.append('car', new Blob(['{}'], { type: 'application/vnd.paw.car.v1+json' }));

      // 2.Act
      await client.post('/cars', fd, {
        accept: 'application/vnd.paw.car.v1+json',
        contentType: 'multipart/form-data',
        extraHeaders: { 'Content-Type': 'multipart/form-data' },
      });

      // 3.Assert
      const init = fetchMock.mock.calls[0][1] as RequestInit;
      expect(init.body).toBe(fd);
      expect((init.headers as Headers).get('Content-Type')).toBeNull();
    });

    it('testParseContentDispositionFileNameReadsQuotedFilename', () => {
      // 2.Act / 3.Assert
      expect(parseContentDispositionFileName('inline; filename="insurance.pdf"')).toBe(
        'insurance.pdf',
      );
    });

    it('testRefreshesExpiredAccessBeforeAuthenticatedRequest', async () => {
      // 1.Arrange
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-10T18:40:00Z'));
      const nowSec = Math.floor(Date.now() / 1000);
      const makeJwt = (payload: Record<string, unknown>) => {
        const encode = (value: unknown) =>
          btoa(JSON.stringify(value))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
        return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.sig`;
      };
      const expiredAccess = makeJwt({ exp: nowSec - 60, tokenType: 'access' });
      const validRefresh = makeJwt({ exp: nowSec + 86_400, tokenType: 'refresh' });
      let storedAccess = expiredAccess;
      let storedRefresh = validRefresh;
      const onTokens = vi.fn((access: string, refresh: string) => {
        storedAccess = access;
        storedRefresh = refresh;
      });
      const c = new HypermediaClient({
        getAccessToken: () => storedAccess,
        getRefreshToken: () => storedRefresh,
        onTokens,
      });
      fetchMock
        .mockResolvedValueOnce(
          makeResponse(
            200,
            { resources: {} },
            { 'X-Access-Token': 'fresh-access', 'X-Refresh-Token': 'fresh-refresh' },
          ),
        )
        .mockResolvedValueOnce(makeResponse(200, { plate: 'ABC123' }));

      // 2.Act
      await c.get('/cars/1', { accept: 'application/vnd.paw.car.v1+json' });

      // 3.Assert
      expect(fetchMock).toHaveBeenCalledTimes(2);
      const refreshInit = fetchMock.mock.calls[0][1] as RequestInit;
      expect((refreshInit.headers as Headers).get('Authorization')).toBe(`Bearer ${validRefresh}`);
      const mainInit = fetchMock.mock.calls[1][1] as RequestInit;
      expect((mainInit.headers as Headers).get('Authorization')).toBe('Bearer fresh-access');
      expect(onTokens).toHaveBeenCalledWith('fresh-access', 'fresh-refresh');
      vi.useRealTimers();
    });
  });
});
