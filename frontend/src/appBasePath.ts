/**
 * Context path del WAR sin barra final: `/webapp` en Jetty local;
 * en Pampero el public path es `/paw-2026a-08` (build con {@code npm run build:pampero}).
 * Vite inyecta {@link import.meta.env.BASE_URL} según {@code --base} / {@code VITE_BASE}.
 */
export function appBasePath(): string {
  const base = import.meta.env.BASE_URL;
  if (!base || base === '/') {
    return '';
  }
  return base.endsWith('/') ? base.slice(0, -1) : base;
}
