// Parseo bidireccional filtros <-> URLSearchParams para /buscar.
// Es la única lógica no trivial de la feature, por eso vive aislada y testeada
// (browse.search.* sincroniza con la URL via useSearchParams en SearchPage).

import {
  CAR_SORTS,
  CAR_TYPES,
  POWERTRAINS,
  TRANSMISSIONS,
  type CarSort,
  type CarType,
  type Powertrain,
  type Transmission,
} from './types';

export interface SearchFilters {
  q?: string;
  category?: CarType;
  transmission?: Transmission;
  powertrain?: Powertrain;
  priceMin?: number;
  priceMax?: number;
  rating?: number;
  neighborhoodId?: number;
  from?: string; // YYYY-MM-DD
  until?: string; // YYYY-MM-DD
  sort?: CarSort;
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function inEnum<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
  return value != null && (allowed as readonly string[]).includes(value) ? (value as T) : undefined;
}

/** number finito >= 0, si no undefined. */
function toNonNegativeNumber(value: string | null): number | undefined {
  if (value == null || value.trim() === '') return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

/** entero positivo (ids), si no undefined. */
function toPositiveInt(value: string | null): number | undefined {
  const n = toNonNegativeNumber(value);
  return n !== undefined && Number.isInteger(n) && n > 0 ? n : undefined;
}

function toRating(value: string | null): number | undefined {
  const n = toNonNegativeNumber(value);
  return n !== undefined && n >= 0 && n <= 5 ? n : undefined;
}

function toIsoDate(value: string | null): string | undefined {
  return value != null && ISO_DATE.test(value) ? value : undefined;
}

/**
 * Lee filtros desde los search params de la URL. Tolerante: valores inválidos o
 * fuera de rango se descartan (no rompen la pantalla). Strings vacíos se omiten.
 */
export function parseFilters(params: URLSearchParams): SearchFilters {
  const q = params.get('q')?.trim();
  const filters: SearchFilters = {
    q: q ? q : undefined,
    category: inEnum(params.get('category'), CAR_TYPES),
    transmission: inEnum(params.get('transmission'), TRANSMISSIONS),
    powertrain: inEnum(params.get('powertrain'), POWERTRAINS),
    priceMin: toNonNegativeNumber(params.get('priceMin')),
    priceMax: toNonNegativeNumber(params.get('priceMax')),
    rating: toRating(params.get('rating')),
    neighborhoodId: toPositiveInt(params.get('neighborhoodId')),
    from: toIsoDate(params.get('from')),
    until: toIsoDate(params.get('until')),
    sort: inEnum(params.get('sort'), CAR_SORTS),
  };
  // Normalización: si min > max, descarto ambos (rango incoherente).
  if (
    filters.priceMin !== undefined &&
    filters.priceMax !== undefined &&
    filters.priceMin > filters.priceMax
  ) {
    delete filters.priceMin;
    delete filters.priceMax;
  }
  return filters;
}

/**
 * Serializa filtros a un objeto plano de strings para la URL (y para el query
 * param de la API, que comparten forma). Omite undefined/vacíos.
 */
export function filtersToParams(filters: SearchFilters): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(filters)) {
    if (value === undefined || value === null) continue;
    const s = String(value).trim();
    if (s === '') continue;
    out[key] = s;
  }
  return out;
}

/** Igual que filtersToParams pero devuelve URLSearchParams (para setSearchParams). */
export function filtersToSearchParams(filters: SearchFilters): URLSearchParams {
  return new URLSearchParams(filtersToParams(filters));
}

/** true si no hay ningún filtro activo (para mostrar estado inicial/empty). */
export function isEmptyFilters(filters: SearchFilters): boolean {
  return Object.values(filters).every((v) => v === undefined || v === null || v === '');
}
