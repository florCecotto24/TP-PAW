import { vi } from 'vitest';

/**
 * Origen del backend local en tests (Jetty/Tomcat en :8080, igual que vite.config proxy).
 * Solo fixtures de unit tests: en producción {@link resolveApiUrl} usa
 * {@code window.location.origin} del navegador (dominio/IP real del deploy).
 */
export const DEV_ORIGIN = 'http://localhost:8080';

/** URL absoluta same-origin para escenarios de test. */
export function devOriginUrl(path: string): string {
  return `${DEV_ORIGIN}${path.startsWith('/') ? path : `/${path}`}`;
}

/** Stub mínimo de `window` alineado con {@link DEV_ORIGIN}. */
export function stubDevWindow(): void {
  vi.stubGlobal('window', { location: { origin: DEV_ORIGIN } });
}
