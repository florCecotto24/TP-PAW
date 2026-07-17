import { create } from 'zustand';
import { ApiError, HypermediaClient, type TokenAccessors } from '../api/client';
import { canonicalApiUserPath } from '../api/uri';
import { MediaTypes } from '../api/mediaTypes';
import type { UserDto } from '../api/types';
import i18n, { isSupported, type Locale } from '../i18n';
import { queryClient } from '../queryClient';

// =============================================================================
// Session store (Zustand) — fuente de verdad de auth (LINEAMIENTOS §1.8)
// -----------------------------------------------------------------------------
// Guarda el par access/refresh, la URN del usuario logueado (descubierta por el
// Link rel="authenticated-user", NO por /me — decisión D2) y el UserDto.
//
// PERSISTENCIA: tokens + URN en localStorage para sobrevivir refresh de página.
//   Trade-off (§1.8): localStorage es legible por JS, así que es vulnerable a
//   XSS (un script inyectado podría robar los tokens). La alternativa —cookies
//   HttpOnly— no aplica acá porque la cátedra exige auth STATELESS por header
//   Authorization (no cookies/sesión). Se asume CSP/escape de salida como
//   mitigación de XSS. El refresh token es de vida larga: en un escenario real
//   convendría acortarla o rotarla; acá se sigue el contrato dado.
// =============================================================================

const STORAGE_KEY = 'ryden.session';
const SESSION_SCHEMA_VERSION = 1;

export type SessionStatus = 'anonymous' | 'authenticating' | 'authenticated' | 'error';

interface PersistedSession {
  schemaVersion?: number;
  accessToken: string | null;
  refreshToken: string | null;
  currentUserUri: string | null;
}

export interface SessionState extends PersistedSession {
  currentUser: UserDto | null;
  status: SessionStatus;
  error: string | null;

  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  /** Persiste un par de tokens recién emitido (Basic o refresh). */
  applyTokens: (access: string, refresh: string) => void;
  /** Rehidrata tokens/URN desde localStorage al bootear la app. */
  loadFromStorage: () => void;
}

function writeStorage(s: PersistedSession): void {
  try {
    if (s.accessToken && s.refreshToken && s.currentUserUri) {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ ...s, schemaVersion: SESSION_SCHEMA_VERSION }),
      );
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    /* localStorage no disponible (SSR/test sin jsdom): no-op */
  }
}

function readStorage(): PersistedSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as PersistedSession;
    if (parsed.schemaVersion !== SESSION_SCHEMA_VERSION) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.currentUserUri) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

/**
 * Aplica a la SPA la preferencia de idioma del usuario logueado (`latestLocale`, persistida
 * server-side) para que siga al usuario entre dispositivos. No-op si es null/ausente, no soportada,
 * o si ya coincide con el idioma actual (evita un changeLanguage redundante).
 *
 * Se invoca SOLO en el login explícito (no en la rehidratación de boot): así un refresh de página
 * no parpadea ni pisa la elección local guardada en localStorage; el server gana al loguear.
 */
function hydrateLocaleFromUser(user: UserDto | null): void {
  const locale = user?.latestLocale;
  if (locale && isSupported(locale) && i18n.language !== locale) {
    void i18n.changeLanguage(locale);
  }
}

/** Invalidates in-flight {@link fetchCurrentUser} calls (login/logout must win over boot rehydration). */
let userFetchSeq = 0;

function fetchCurrentUser(
  uri: string,
  get: () => SessionState,
  set: (partial: Partial<SessionState>) => void,
): void {
  const seq = ++userFetchSeq;
  const loadUser = (attempt: number): void => {
    if (seq !== userFetchSeq) return;
    sessionClient
      .follow<UserDto>(uri, { accept: MediaTypes.userPrivate })
      .then((res) => {
        if (seq !== userFetchSeq) return;
        set({ currentUser: res.data });
      })
      .catch((err) => {
        if (seq !== userFetchSeq) return;
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          get().logout();
        } else if (attempt < 3) {
          setTimeout(() => loadUser(attempt + 1), 250 * (attempt + 1));
        }
      });
  };
  loadUser(0);
}

function bootstrapCurrentUserFromSession(): void {
  const state = useSessionStore.getState();
  if (state.accessToken && state.refreshToken && state.currentUserUri) {
    fetchCurrentUser(state.currentUserUri, useSessionStore.getState, useSessionStore.setState);
  }
}

export const useSessionStore = create<SessionState>((set, get) => {
  // Hidratación SÍNCRONA al crear el store: si hay tokens persistidos arrancamos
  // ya como 'authenticated' para que RequireAuth no rebote un deep-link protegido
  // a /ingresar antes de que loadFromStorage (que rehidrata el UserDto) corra en
  // el useEffect de App (race de hidratación).
  const persisted = readStorage();
  const hasSession = !!persisted?.accessToken && !!persisted?.refreshToken && !!persisted?.currentUserUri;
  const persistedUserUri = hasSession
    ? canonicalApiUserPath(persisted!.currentUserUri!)
    : null;

  return {
  accessToken: hasSession ? persisted!.accessToken : null,
  refreshToken: hasSession ? persisted!.refreshToken : null,
  currentUserUri: persistedUserUri,
  currentUser: null,
  status: hasSession ? 'authenticated' : 'anonymous',
  error: null,

  applyTokens: (access, refresh) => {
    set({ accessToken: access, refreshToken: refresh });
    writeStorage({
      accessToken: access,
      refreshToken: refresh,
      currentUserUri: get().currentUserUri,
    });
  },

  login: async (email, password) => {
    userFetchSeq++;
    set({ status: 'authenticating', error: null });
    try {
      // 1) Basic -> tokens en headers + Link rel="authenticated-user".
      //    Los callbacks (onTokens/onAuthenticatedUser) actualizan el store.
      await sessionClient.loginBasic(email, password);

      const userUri = get().currentUserUri;
      if (!get().accessToken || !userUri) {
        throw new Error('login.missingCredentials');
      }

      // 2) Seguimos la URN del usuario para traer su UserDto PRIVADO (es el usuario propio: necesita
      //    email/cbu/rol — el navbar/guards leen currentUser.role). El server lo autoriza por ser self.
      const res = await sessionClient.follow<UserDto>(userUri, {
        accept: MediaTypes.userPrivate,
      });
      set({ currentUser: res.data, status: 'authenticated' });
      hydrateLocaleFromUser(res.data);
    } catch (err) {
      get().logout();
      set({ status: 'error', error: err instanceof Error ? err.message : 'login.failed' });
      throw err;
    }
  },

  logout: () => {
    userFetchSeq++;
    sessionClient.clearConditionalGetCache();
    queryClient.clear();
    set({
      accessToken: null,
      refreshToken: null,
      currentUserUri: null,
      currentUser: null,
      status: 'anonymous',
      error: null,
    });
    writeStorage({ accessToken: null, refreshToken: null, currentUserUri: null });
  },

  loadFromStorage: () => {
    const persisted = readStorage();
    if (!persisted?.accessToken || !persisted.refreshToken || !persisted.currentUserUri) {
      return;
    }
    const persistedUserUri = canonicalApiUserPath(persisted.currentUserUri);
    set({
      accessToken: persisted.accessToken,
      refreshToken: persisted.refreshToken,
      currentUserUri: persistedUserUri,
      status: 'authenticated',
    });
    // User DTO rehydration runs once from {@link bootstrapCurrentUserFromSession} after
    // {@link sessionClient} exists; avoid a second parallel fetch here (it could race login).
  },
  };
});

// El cliente lee/escribe tokens a través del store. Es la única instancia que
// el resto de la app usa para hablar con la API.
const tokenAccessors: TokenAccessors = {
  getAccessToken: () => useSessionStore.getState().accessToken,
  getRefreshToken: () => useSessionStore.getState().refreshToken,
  onTokens: (access, refresh) => useSessionStore.getState().applyTokens(access, refresh),
  onAuthenticatedUser: (userUri) => {
    const canonicalUserUri = canonicalApiUserPath(userUri);
    useSessionStore.setState({ currentUserUri: canonicalUserUri });
    const s = useSessionStore.getState();
    writeStorage({
      accessToken: s.accessToken,
      refreshToken: s.refreshToken,
      currentUserUri: canonicalUserUri,
    });
  },
};

export const sessionClient = new HypermediaClient(tokenAccessors);

/**
 * Persiste en el server el idioma elegido (atributo `latestLocale`) cuando hay sesión, para que la
 * preferencia siga al usuario entre dispositivos y `hydrateLocaleFromUser` no la pise con un valor
 * stale en el próximo login. Best-effort: el idioma ya quedó en localStorage (vía el listener de
 * i18n), así que un fallo del PATCH NO debe romper el cambio de idioma. No-op si es anónimo.
 */
export async function persistLocaleToServer(locale: Locale): Promise<void> {
  const { currentUserUri, accessToken, refreshToken } = useSessionStore.getState();
  if (!currentUserUri || (!accessToken && !refreshToken)) {
    return;
  }
  try {
    const res = await sessionClient.patch<UserDto>(
      currentUserUri,
      { latestLocale: locale },
      { accept: MediaTypes.userPrivate, contentType: MediaTypes.user },
    );
    if (res.data && i18n.language === locale) {
      useSessionStore.setState({ currentUser: res.data });
    }
  } catch {
    /* best-effort: la elección ya está en localStorage; no rompemos la UI. */
  }
}

if (typeof window !== 'undefined') {
  queueMicrotask(bootstrapCurrentUserFromSession);
}
