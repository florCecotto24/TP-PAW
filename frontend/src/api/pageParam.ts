// =============================================================================
// Helpers para llevar la PÁGINA al estado de la URL (bookmarkeable).
// -----------------------------------------------------------------------------
// La cátedra exige que el estado navegable (filtros, tabs y PÁGINA) viva en la
// URL. Espejamos la convención de SearchPage: el índice de página en la URL es
// 0-based (`?page=0` = primera) y la capa de datos lo convierte al `page`
// 1-based que espera la API (@QueryParam("page") @DefaultValue("1"), §1.6/§16).
// Estos helpers leen/escriben `?page=N` de forma pura (testeable) y MERGEAN sin
// pisar los filtros.
// =============================================================================

export const PAGE_PARAM = 'page';

/**
 * Índice de página 0-based leído de la URL (espeja SearchPage). Tolerante:
 * ausente, no numérico, <0 o no entero -> 0 (primera). Así un deep-link roto
 * (?page=abc) no rompe la pantalla.
 */
export function pageIndexFromParams(params: URLSearchParams, paramName: string = PAGE_PARAM): number {
  const raw = params.get(paramName);
  if (raw == null || raw.trim() === '') return 0;
  const n = Number(raw);
  return Number.isInteger(n) && n >= 0 ? n : 0;
}

/**
 * Copia de los search params con `?<paramName>=N` seteado (o eliminado si es la
 * página 0, para no ensuciar la URL inicial). MERGEA: conserva los demás params
 * (filtros, tabs). No muta el original.
 */
export function withPageIndex(
  params: URLSearchParams,
  pageIndex: number,
  paramName: string = PAGE_PARAM,
): URLSearchParams {
  const next = new URLSearchParams(params);
  if (pageIndex <= 0) next.delete(paramName);
  else next.set(paramName, String(pageIndex));
  return next;
}
