/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Build de producción 100% estático (Pampero = Tomcat sin Node, sin SSR).
// Tomcat local despliega el WAR como /webapp → `npm run build:tomcat` (--base=/webapp/).
// Jetty dev (`mvn jetty:run`) sirve en / → `npm run build` con base '/'.
// outDir:'dist' + war-plugin copia dist/ a la raíz del WAR (ver PROPUESTA §5).
export default defineConfig({
  plugins: [react()],
  base: process.env.VITE_BASE_PATH ?? '/',
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
      // Tomcat WAR at /webapp (npm run build:tomcat) — dev server at :5173 needs CORS + proxy.
      '/webapp': {
        target: 'http://localhost:8080',
        changeOrigin: true,
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
