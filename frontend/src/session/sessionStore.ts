import { create } from 'zustand';
import { ApiError, HypermediaClient, type TokenAccessors } from '../api/client';
import { MediaTypes } from '../api/mediaTypes';
import type { UserDto } from '../api/types';

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

export type SessionStatus = 'anonymous' | 'authenticating' | 'authenticated' | 'error';

interface PersistedSession {
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
    if (s.accessToken && s.refreshToken) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
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
    return raw ? (JSON.parse(raw) as PersistedSession) : null;
  } catch {
    return null;
  }
}

export const useSessionStore = create<SessionState>((set, get) => {
  // Hidratación SÍNCRONA al crear el store: si hay tokens persistidos arrancamos
  // ya como 'authenticated' para que RequireAuth no rebote un deep-link protegido
  // a /ingresar antes de que loadFromStorage (que rehidrata el UserDto) corra en
  // el useEffect de App (race de hidratación).
  const persisted = readStorage();
  const hasTokens = !!persisted?.accessToken && !!persisted?.refreshToken;
  return {
  accessToken: hasTokens ? persisted!.accessToken : null,
  refreshToken: hasTokens ? persisted!.refreshToken : null,
  currentUserUri: hasTokens ? persisted!.currentUserUri : null,
  currentUser: null,
  status: hasTokens ? 'authenticated' : 'anonymous',
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
    } catch (err) {
      get().logout();
      set({ status: 'error', error: err instanceof Error ? err.message : 'login.failed' });
      throw err;
    }
  },

  logout: () => {
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
    if (!persisted?.accessToken || !persisted.refreshToken) {
      return;
    }
    set({
      accessToken: persisted.accessToken,
      refreshToken: persisted.refreshToken,
      currentUserUri: persisted.currentUserUri,
      status: 'authenticated',
    });
    // Rehidratar el UserDto desde la URN guardada (el navbar/guards/admin lo
    // necesitan tras un refresh). El cliente ya reintenta el access token vencido
    // con el refresh. SOLO cerramos sesión si el server rechaza definitivamente
    // las credenciales (401/403): un error transitorio (red, o el clásico
    // net::ERR_ABORTED del fetch disparado durante el boot/deep-link) NO debe
    // destruir una sesión válida y rebotar al login — en ese caso reintentamos.
    const uri = persisted.currentUserUri;
    if (uri) {
      const loadUser = (attempt: number): void => {
        sessionClient
          .follow<UserDto>(uri, { accept: MediaTypes.userPrivate })
          .then((res) => set({ currentUser: res.data }))
          .catch((err) => {
            if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
              get().logout();
            } else if (attempt < 3) {
              setTimeout(() => loadUser(attempt + 1), 250 * (attempt + 1));
            }
          });
      };
      loadUser(0);
    }
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
    useSessionStore.setState({ currentUserUri: userUri });
    const s = useSessionStore.getState();
    writeStorage({
      accessToken: s.accessToken,
      refreshToken: s.refreshToken,
      currentUserUri: userUri,
    });
  },
};

export const sessionClient = new HypermediaClient(tokenAccessors);
