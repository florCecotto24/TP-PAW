/** Agrupa un array en bloques de tamaño fijo (p.ej. 4 autos por slide del carrusel). */
export function chunk<T>(items: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    out.push(items.slice(i, i + size));
  }
  return out;
}
