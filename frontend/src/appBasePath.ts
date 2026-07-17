/**
 * Context path del WAR sin barra final (`/paw-2026a-08` en package y en Pampero).
 * Vite inyecta {@link import.meta.env.BASE_URL} según el {@code base} del build
 * ({@code npm run build} / {@code mvn package}). En {@code npm run dev} suele ser {@code ''}.
 */
export function appBasePath(): string {
  const base = import.meta.env.BASE_URL;
  if (!base || base === '/') {
    return '';
  }
  return base.endsWith('/') ? base.slice(0, -1) : base;
}
