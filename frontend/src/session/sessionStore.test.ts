import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useSessionStore, sessionClient } from './sessionStore';
import { parseLinkHeader } from '../api/client';

function makeResponse(
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): Response {
  const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body);
  return new Response(status === 204 ? null : text, { status, headers: new Headers(headers) });
}

const USER_DTO = {
  forename: 'Ana',
  surname: 'B',
  email: 'a@b.com',
  emailVerified: true,
  licenseValidated: false,
  identityValidated: false,
  blocked: false,
  role: 'user',
  links: { self: '/users/42' },
};

const fetchMock = vi.fn();

function resetStore() {
  useSessionStore.setState({
    accessToken: null,
    refreshToken: null,
    currentUserUri: null,
    currentUser: null,
    status: 'anonymous',
    error: null,
  });
  localStorage.clear();
}

describe('sessionStore', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    resetStore();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('login sends Basic and stores access + refresh + currentUserUri from headers, then loads the user', async () => {
    // 1.Arrange
    fetchMock
      .mockResolvedValueOnce(
        makeResponse(
          200,
          { resources: {} },
          {
            'X-Access-Token': 'access-1',
            'X-Refresh-Token': 'refresh-1',
            Link: '</users/42>; rel="authenticated-user"',
          },
        ),
      )
      .mockResolvedValueOnce(makeResponse(200, USER_DTO));

    // 2.Act
    await useSessionStore.getState().login('a@b.com', 'secret');

    // 3.Assert
    const state = useSessionStore.getState();
    expect(state.accessToken).toBe('access-1');
    expect(state.refreshToken).toBe('refresh-1');
    expect(state.currentUserUri).toBe('/users/42');
    expect(state.currentUser?.forename).toBe('Ana');
    expect(state.status).toBe('authenticated');

    const firstInit = fetchMock.mock.calls[0][1] as RequestInit;
    expect((firstInit.headers as Headers).get('Authorization')).toMatch(/^Basic /);
    expect(fetchMock.mock.calls[1][0]).toBe('/users/42');

    const persisted = JSON.parse(localStorage.getItem('ryden.session') ?? '{}');
    expect(persisted.accessToken).toBe('access-1');
    expect(persisted.currentUserUri).toBe('/users/42');
  });

  it('a request with an expired access token (401) retries with the refresh token and persists the new pair', async () => {
    // 1.Arrange
    useSessionStore.setState({
      accessToken: 'expired',
      refreshToken: 'refresh-old',
      currentUserUri: '/users/42',
      status: 'authenticated',
    });
    fetchMock
      .mockResolvedValueOnce(makeResponse(401, { status: 401, code: 'unauthorized' }))
      .mockResolvedValueOnce(
        makeResponse(200, USER_DTO, {
          'X-Access-Token': 'access-2',
          'X-Refresh-Token': 'refresh-2',
        }),
      );

    // 2.Act
    const res = await sessionClient.get('/users/42');

    // 3.Assert
    expect(res.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect((fetchMock.mock.calls[1][1] as RequestInit).headers).toBeDefined();
    expect((fetchMock.mock.calls[1][1] as RequestInit).headers as Headers).toBeInstanceOf(Headers);
    expect(((fetchMock.mock.calls[1][1] as RequestInit).headers as Headers).get('Authorization')).toBe(
      'Bearer refresh-old',
    );

    const state = useSessionStore.getState();
    expect(state.accessToken).toBe('access-2');
    expect(state.refreshToken).toBe('refresh-2');
  });

  it('logout clears tokens, user and localStorage', () => {
    // 1.Arrange
    useSessionStore.setState({
      accessToken: 'a',
      refreshToken: 'r',
      currentUserUri: '/users/42',
      currentUser: USER_DTO as never,
      status: 'authenticated',
    });
    localStorage.setItem('ryden.session', JSON.stringify({ accessToken: 'a' }));

    // 2.Act
    useSessionStore.getState().logout();

    // 3.Assert
    const state = useSessionStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.currentUserUri).toBeNull();
    expect(state.currentUser).toBeNull();
    expect(state.status).toBe('anonymous');
    expect(localStorage.getItem('ryden.session')).toBeNull();
  });

  it('loadFromStorage rehydrates tokens and URN', () => {
    // 1.Arrange
    localStorage.setItem(
      'ryden.session',
      JSON.stringify({ accessToken: 'a', refreshToken: 'r', currentUserUri: '/users/7' }),
    );

    // 2.Act
    useSessionStore.getState().loadFromStorage();

    // 3.Assert
    const state = useSessionStore.getState();
    expect(state.accessToken).toBe('a');
    expect(state.refreshToken).toBe('r');
    expect(state.currentUserUri).toBe('/users/7');
    expect(state.status).toBe('authenticated');
  });

  it('parseLinkHeader parses next/last rels (sanity for pagination navigation)', () => {
    // 1.Arrange
    const header = '</cars?page=2>; rel="next", </cars?page=8>; rel="last"';

    // 2.Act
    const rels = parseLinkHeader(header);

    // 3.Assert
    expect(rels.next).toBe('/cars?page=2');
    expect(rels.last).toBe('/cars?page=8');
  });
});
