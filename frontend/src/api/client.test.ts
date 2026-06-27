import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  HypermediaClient,
  parseLinkHeader,
  extractPageLinks,
  type TokenAccessors,
} from './client';

function makeResponse(
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): Response {
  const h = new Headers(headers);
  const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body);
  return new Response(status === 204 ? null : text, { status, headers: h });
}

describe('parseLinkHeader', () => {
  it('parses rel next and last from an RFC 5988 header', () => {
    // 1.Arrange
    const header =
      '</cars?page=2&category=suv>; rel="next", </cars?page=5&category=suv>; rel="last"';

    // 2.Act
    const rels = parseLinkHeader(header);

    // 3.Assert
    expect(rels.next).toBe('/cars?page=2&category=suv');
    expect(rels.last).toBe('/cars?page=5&category=suv');
  });

  it('parses all four pagination rels', () => {
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

  it('handles rel without quotes and the authenticated-user rel', () => {
    // 2.Act
    const rels = parseLinkHeader('</users/42>; rel=authenticated-user');

    // 3.Assert
    expect(rels['authenticated-user']).toBe('/users/42');
  });

  it('returns empty for null/empty', () => {
    // 2.Act / 3.Assert
    expect(parseLinkHeader(null)).toEqual({});
    expect(parseLinkHeader('')).toEqual({});
  });
});

describe('extractPageLinks', () => {
  it('combines Link rels with X-Total-Count', () => {
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

describe('HypermediaClient 401 -> refresh flow', () => {
  let tokens: { access: string | null; refresh: string | null };
  let onTokens: ReturnType<typeof vi.fn>;
  let accessors: TokenAccessors;
  let client: HypermediaClient;
  const fetchMock = vi.fn();

  beforeEach(() => {
    tokens = { access: 'expired-access', refresh: 'valid-refresh' };
    onTokens = vi.fn((a: string, r: string) => {
      tokens.access = a;
      tokens.refresh = r;
    });
    accessors = {
      getAccessToken: () => tokens.access,
      getRefreshToken: () => tokens.refresh,
      onTokens,
    };
    client = new HypermediaClient(accessors);
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('retries once with the refresh token after a 401 and persists the new pair', async () => {
    // 1.Arrange
    fetchMock
      .mockResolvedValueOnce(makeResponse(401, { status: 401, code: 'unauthorized' }))
      .mockResolvedValueOnce(
        makeResponse(
          200,
          [{ links: { self: '/reservations/1' } }],
          { 'X-Access-Token': 'new-access', 'X-Refresh-Token': 'new-refresh' },
        ),
      );

    // 2.Act
    const res = await client.get('/reservations?riderId=42');

    // 3.Assert
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const firstInit = fetchMock.mock.calls[0][1] as RequestInit;
    expect((firstInit.headers as Headers).get('Authorization')).toBe('Bearer expired-access');
    const secondInit = fetchMock.mock.calls[1][1] as RequestInit;
    expect((secondInit.headers as Headers).get('Authorization')).toBe('Bearer valid-refresh');
    expect(onTokens).toHaveBeenCalledWith('new-access', 'new-refresh');
    expect(res.status).toBe(200);
  });

  it('does not retry when there is no refresh token', async () => {
    // 1.Arrange
    tokens.refresh = null;
    fetchMock.mockResolvedValueOnce(makeResponse(401, { status: 401 }));

    // 2.Act / 3.Assert
    await expect(client.get('/reservations')).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('absorbs the authenticated-user Link on login (Basic)', async () => {
    // 1.Arrange
    const onAuthenticatedUser = vi.fn();
    const c = new HypermediaClient({ ...accessors, onAuthenticatedUser });
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

  it('testFollowUsesAbsolutePaginationLinksWithoutDoublePrefix', async () => {
    // 1.Arrange
    fetchMock.mockResolvedValueOnce(
      makeResponse(200, [{ links: { self: '/cars/1' } }], {
        Link: '<http://localhost:8080/webapp/cars?page=2>; rel="next"',
      }),
    );

    // 2.Act
    await client.follow('http://localhost:8080/webapp/cars?page=2', {
      accept: 'application/vnd.paw.car.v1+json',
    });

    // 3.Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8080/webapp/cars?page=2');
  });
});
