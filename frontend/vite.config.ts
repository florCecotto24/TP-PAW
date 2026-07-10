/// <reference types="vitest" />
import type { IncomingMessage } from 'node:http';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/** Vite dev + base `/webapp/`: do not proxy the SPA shell or HMR to the API backend. */
function bypassWebappDevProxy(req: IncomingMessage): string | undefined {
  const url = req.url ?? '';
  const accept = req.headers.accept ?? '';
  if (accept.includes('text/html')) {
    return url;
  }
  if (
    url.includes('/@vite/') ||
    url.includes('/@react-refresh') ||
    url.includes('/@id/') ||
    url.includes('/src/') ||
    url.includes('/node_modules/')
  ) {
    return url;
  }
  if (/\.(tsx?|jsx?|css|map|svg|png|jpe?g|gif|webp|ico|woff2?)(\?|$)/i.test(url)) {
    return url;
  }
  return undefined;
}

// Build de producción 100% estático (Pampero = Tomcat sin Node, sin SSR).
// El WAR se despliega como /webapp → base '/webapp/' (LINEAMIENTOS: configurar el bundler).
// outDir:'dist' + war-plugin copia dist/ a la raíz del WAR.
// Vitest corre con mode 'test' por defecto: ahí forzamos base '/' porque los
// tests ejercitan lógica pura (resolveApiUrl, sessionStore) contra URNs
// relativas tal cual las devuelve la API, sin querer el prefijo de despliegue.
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: mode === 'test' ? '/' : '/webapp/',
  build: {
    outDir: 'dist',
    assetsDir: 'public',
    minify: 'esbuild', // minifica JS + CSS
    rollupOptions: {
      output: {
        entryFileNames: 'public/[name].[hash].js',
        chunkFileNames: 'public/[name].[hash].js',
        assetFileNames: 'public/[name].[hash][extname]',
      },
    },
  },
  server: {
    // Dev server: proxy unified API mount (/api) to the backend (Jetty context /).
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // dev:war (base /webapp/): strip WAR context when Jetty runs with server.servlet.context-path=/.
      '/webapp/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/webapp(?=\/|$)/, '') || '/',
      },
      '/webapp': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/webapp(?=\/|$)/, '') || '/',
        bypass: bypassWebappDevProxy,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
}));
