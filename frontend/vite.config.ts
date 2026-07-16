/// <reference types="vitest" />
import type { IncomingMessage } from 'node:http';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/** Local Jetty context (`webapp.war` → `/webapp`). */
const LOCAL_CONTEXT = '/webapp';

function normalizeBase(path: string): string {
  const trimmed = path.trim();
  if (!trimmed || trimmed === '/') {
    return '/';
  }
  return trimmed.endsWith('/') ? trimmed : `${trimmed}/`;
}

/**
 * base del bundler:
 * - test → `/` (URNs relativas en Vitest)
 * - {@code VITE_BASE} → override (Pampero: `/paw-2026a-08/`)
 * - default → `/webapp/` (Jetty local)
 */
function resolveBase(mode: string): string {
  if (mode === 'test') {
    return '/';
  }
  if (process.env.VITE_BASE) {
    return normalizeBase(process.env.VITE_BASE);
  }
  return normalizeBase(LOCAL_CONTEXT);
}

/** Vite dev: do not proxy the SPA shell or HMR to the API backend. */
function bypassLocalDevProxy(req: IncomingMessage): string | undefined {
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

// Build estático (Pampero = Tomcat sin Node). outDir dist + war-plugin → raíz del WAR.
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: resolveBase(mode),
  build: {
    outDir: 'dist',
    assetsDir: 'public',
    minify: 'esbuild',
    rollupOptions: {
      output: {
        entryFileNames: 'public/[name].[hash].js',
        chunkFileNames: 'public/[name].[hash].js',
        assetFileNames: 'public/[name].[hash][extname]',
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: `http://localhost:8080${LOCAL_CONTEXT}`,
        changeOrigin: true,
      },
      [`${LOCAL_CONTEXT}/api`]: {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      [LOCAL_CONTEXT]: {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass: bypassLocalDevProxy,
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
