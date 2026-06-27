/**
 * Context path del WAR sin barra final: '' en Jetty (/), '/webapp' en Tomcat (/webapp/).
 * Vite inyecta {@link import.meta.env.BASE_URL} según {@code --base} en el build.
 */
export function appBasePath(): string {
  const base = import.meta.env.BASE_URL;
  if (!base || base === '/') {
    return '';
  }
  return base.endsWith('/') ? base.slice(0, -1) : base;
}
