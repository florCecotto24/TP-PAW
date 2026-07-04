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
export default defineConfig({
  plugins: [react()],
  base: '/webapp/',
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
    // Dev server: proxy de los recursos de la API al backend (mismo origen en prod).
    // Se listan los prefijos de recursos REST (la API NO tiene prefijo /api).
    proxy: {
      // Auth probe (Basic → JWT): GET / with Accept application/vnd.paw.* (npm run dev, base /).
      '/': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          const accept = req.headers.accept ?? '';
          if (accept.includes('application/vnd.paw')) {
            return undefined;
          }
          return req.url;
        },
      },
      // Local Jetty (context /): API lives at /cars, not /webapp/cars — strip prefix on proxy.
      // Use `npm run dev` (--base=/). For Tomcat at /webapp use `npm run dev:war` without rewrite.
      '/webapp': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/webapp(?=\/|$)/, '') || '/',
        bypass: bypassWebappDevProxy,
      },
      '/users': 'http://localhost:8080',
      '/cars': 'http://localhost:8080',
      '/reservations': 'http://localhost:8080',
      '/brands': 'http://localhost:8080',
      '/models': 'http://localhost:8080',
      '/neighborhoods': 'http://localhost:8080',
      '/credentials': 'http://localhost:8080',
      '/image': 'http://localhost:8080',
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
