// La configuración de tests vive en vite.config.ts (bloque `test`).
// Este archivo re-exporta esa config para que `vitest` la encuentre de forma
// explícita y para satisfacer la referencia de tsconfig.node.json.
export { default } from './vite.config';
