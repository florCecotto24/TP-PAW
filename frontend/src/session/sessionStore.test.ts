import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { persistLocaleToServer, useSessionStore, sessionClient } from './sessionStore';
import { parseLinkHeader } from '../api/client';
import i18n from '../i18n';

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
const memoryStorage = new Map<string, string>();

function installMemoryLocalStorage(): void {
  memoryStorage.clear();
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => memoryStorage.get(key) ?? null,
    setItem: (key: string, value: string) => {
      memoryStorage.set(key, String(value));
    },
    removeItem: (key: string) => {
      memoryStorage.delete(key);
    },
    clear: () => {
      memoryStorage.clear();
    },
    get length() {
      return memoryStorage.size;
    },
    key: (index: number) => [...memoryStorage.keys()][index] ?? null,
  });
}

function resetStore() {
  useSessionStore.setState({
    accessToken: null,
    refreshToken: null,
    currentUserUri: null,
    currentUser: null,
    status: 'anonymous',
    error: null,
  });
  memoryStorage.clear();
}

describe('sessionStore', () => {
  beforeEach(() => {
    installMemoryLocalStorage();
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    resetStore();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('testLoginSendsBasicAndStoresTokensThenLoadsUser', async () => {
    // 1.Arrange
    const authUserUrl = 'http://localhost:8080/webapp/api/users/42';
    fetchMock
      .mockResolvedValueOnce(
        makeResponse(
          200,
          { resources: {} },
          {
            'X-Access-Token': 'access-1',
            'X-Refresh-Token': 'refresh-1',
            Link: `<${authUserUrl}>; rel="authenticated-user"`,
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
    expect(state.currentUserUri).toContain('/users/42');
    expect(state.currentUser?.forename).toBe('Ana');
    expect(state.status).toBe('authenticated');

    const firstInit = fetchMock.mock.calls[0][1] as RequestInit;
    expect((firstInit.headers as Headers).get('Authorization')).toMatch(/^Basic /);
    expect(String(fetchMock.mock.calls[1][0])).toContain('/users/42');

    const persisted = JSON.parse(localStorage.getItem('ryden.session') ?? '{}');
    expect(persisted.accessToken).toBe('access-1');
    expect(String(persisted.currentUserUri)).toContain('/users/42');
  });

  it('testRequestWithExpiredAccessRetriesWithRefreshAndPersistsNewPair', async () => {
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
      )
      // Third attempt with fresh access after refresh rotates the pair (client.ts).
      .mockResolvedValueOnce(makeResponse(200, USER_DTO));

    // 2.Act
    const res = await sessionClient.get('/users/42');

    // 3.Assert
    expect(res.status).toBe(200);
    expect(fetchMock.mock.calls.length).toBeGreaterThanOrEqual(2);
    const refreshCall = fetchMock.mock.calls.find(
      (call) =>
        ((call[1] as RequestInit).headers as Headers).get('Authorization') === 'Bearer refresh-old',
    );
    expect(refreshCall).toBeDefined();

    const state = useSessionStore.getState();
    expect(state.accessToken).toBe('access-2');
    expect(state.refreshToken).toBe('refresh-2');
  });

  it('testLogoutClearsTokensUserAndLocalStorage', () => {
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

  it('testLoadFromStorageRehydratesTokensAndUrn', () => {
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
    expect(state.currentUserUri).toContain('/users/7');
    expect(state.status).toBe('authenticated');
  });

  it('testParseLinkHeaderReadsNextAndLastRels', () => {
    // 1.Arrange / 2.Act
    const rels = parseLinkHeader('</cars?page=2>; rel="next", </cars?page=8>; rel="last"');
    // 3.Assert
    expect(rels.next).toBe('/cars?page=2');
    expect(rels.last).toBe('/cars?page=8');
  });

  it('testLoginHydratesLocaleFromUserLatestLocale', async () => {
    // 1.Arrange
    const authUserUrl = 'http://localhost:8080/webapp/api/users/42';
    await i18n.changeLanguage('es');
    fetchMock
      .mockResolvedValueOnce(
        makeResponse(
          200,
          { resources: {} },
          {
            'X-Access-Token': 'access-1',
            'X-Refresh-Token': 'refresh-1',
            Link: `<${authUserUrl}>; rel="authenticated-user"`,
          },
        ),
      )
      .mockResolvedValueOnce(makeResponse(200, { ...USER_DTO, latestLocale: 'en' }));

    // 2.Act
    await useSessionStore.getState().login('a@b.com', 'secret');

    // 3.Assert
    expect(i18n.language).toMatch(/^en/);
  });

  it('testPersistLocaleToServerIsNoOpWhenAnonymous', async () => {
    // 1.Arrange — anonymous store
    // 2.Act
    await persistLocaleToServer('en');
    // 3.Assert
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('testPersistLocaleToServerPatchesLatestLocaleWhenAuthenticated', async () => {
    // 1.Arrange
    useSessionStore.setState({
      accessToken: 'a',
      refreshToken: 'r',
      currentUserUri: '/webapp/api/users/42',
      currentUser: USER_DTO as never,
      status: 'authenticated',
    });
    await i18n.changeLanguage('en');
    fetchMock.mockResolvedValueOnce(
      makeResponse(200, { ...USER_DTO, latestLocale: 'en' }),
    );

    // 2.Act
    await persistLocaleToServer('en');

    // 3.Assert
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe('PATCH');
    expect(JSON.parse(String(init.body))).toEqual({ latestLocale: 'en' });
    expect(useSessionStore.getState().currentUser?.latestLocale).toBe('en');
  });
});
