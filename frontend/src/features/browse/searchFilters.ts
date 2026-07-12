// Bidirectional mapping between URL search params and SearchFilters for /search.

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
import { apiSortToJspSort, jspSortToApiSort } from './exploreSearch';
import { daysInFlexMonth } from './flexibleSearch';

export interface SearchFilters {
  q?: string;
  category?: CarType;
  transmission?: Transmission;
  powertrain?: Powertrain;
  priceMin?: number;
  priceMax?: number;
  /** Posición vs mercado del mismo modelo (mismas bandas que el badge de la card). */
  priceMarket?: 'below_market' | 'at_market' | 'above_market';
  rating?: number;
  neighborhoodId?: number;
  from?: string; // YYYY-MM-DD
  until?: string; // YYYY-MM-DD
  flexible?: boolean;
  flexMonth?: string; // YYYY-MM
  flexDays?: number;
  sort?: CarSort;
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const FLEX_MONTH = /^\d{4}-\d{2}$/;

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

function toFlexMonth(value: string | null): string | undefined {
  return value != null && FLEX_MONTH.test(value) ? value : undefined;
}

function toFlexDays(value: string | null, maxDays = 31): number | undefined {
  if (value == null || value.trim() === '') return undefined;
  const n = Number(value);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < 1) return undefined;
  return Math.min(n, maxDays);
}

function toIsoDate(value: string | null): string | undefined {
  return value != null && ISO_DATE.test(value) ? value : undefined;
}

const PRICE_MARKET_VALUES = ['below_market', 'at_market', 'above_market'] as const;

function toPriceMarket(
  value: string | null,
): SearchFilters['priceMarket'] | undefined {
  return value != null && (PRICE_MARKET_VALUES as readonly string[]).includes(value)
    ? (value as SearchFilters['priceMarket'])
    : undefined;
}

/**
 * Lee filtros desde los search params de la URL. Tolerante: valores inválidos o
 * fuera de rango se descartan (no rompen la pantalla). Strings vacíos se omiten.
 */
export function parseFilters(params: URLSearchParams): SearchFilters {
  const q =
    params.get('query')?.trim() ||
    params.get('q')?.trim();
  const sortRaw = params.get('sort');
  let sort: CarSort | undefined;
  if (sortRaw) {
    if ((CAR_SORTS as readonly string[]).includes(sortRaw)) {
      sort = sortRaw as CarSort;
    } else {
      sort = jspSortToApiSort(sortRaw);
    }
  }
  const flexible = params.get('flexible') === 'true';
  const flexMonth = toFlexMonth(params.get('flexMonth'));
  const flexDaysRaw = params.get('flexDays');
  const flexDays = flexMonth ? toFlexDays(flexDaysRaw, daysInFlexMonth(flexMonth)) : undefined;

  const filters: SearchFilters = {
    q: q ? q : undefined,
    category: inEnum(params.get('category'), CAR_TYPES),
    transmission: inEnum(params.get('transmission'), TRANSMISSIONS),
    powertrain: inEnum(params.get('powertrain'), POWERTRAINS),
    priceMin: toNonNegativeNumber(params.get('priceMin')),
    priceMax: toNonNegativeNumber(params.get('priceMax')),
    priceMarket: toPriceMarket(params.get('priceMarket')),
    rating: toRating(params.get('rating')),
    neighborhoodId: toPositiveInt(params.get('neighborhoodId')),
    flexible: flexible || undefined,
    flexMonth: flexible ? flexMonth : undefined,
    flexDays: flexible ? flexDays : undefined,
    from: flexible ? undefined : toIsoDate(params.get('from')),
    until: flexible ? undefined : toIsoDate(params.get('until')),
    sort,
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
 * Serializa filtros para la API REST (`q`, sort API, …).
 */
export function filtersToApiParams(filters: SearchFilters): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(filters)) {
    if (value === undefined || value === null) continue;
    if (key === 'from' || key === 'until') {
      if (filters.flexible) continue;
    }
    if ((key === 'flexMonth' || key === 'flexDays') && !filters.flexible) continue;
    const s = String(value).trim();
    if (s === '') continue;
    if (key === 'q') {
      out.q = s;
    } else if (key === 'flexible') {
      if (value === true) out.flexible = 'true';
    } else {
      out[key] = s;
    }
  }
  return out;
}

/**
 * Serializa filtros a URL (paridad JSP: `query`, sort `date,desc`, …).
 */
export function filtersToSearchParams(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('query', filters.q);
  if (filters.category) params.set('category', filters.category);
  if (filters.transmission) params.set('transmission', filters.transmission);
  if (filters.powertrain) params.set('powertrain', filters.powertrain);
  if (filters.priceMin !== undefined) params.set('priceMin', String(filters.priceMin));
  if (filters.priceMax !== undefined) params.set('priceMax', String(filters.priceMax));
  if (filters.priceMarket) params.set('priceMarket', filters.priceMarket);
  if (filters.rating !== undefined) params.set('rating', String(filters.rating));
  if (filters.neighborhoodId !== undefined) params.set('neighborhoodId', String(filters.neighborhoodId));
  if (filters.flexible && filters.flexMonth) {
    params.set('flexible', 'true');
    params.set('flexMonth', filters.flexMonth);
    if (filters.flexDays !== undefined) params.set('flexDays', String(filters.flexDays));
  } else {
    if (filters.from) params.set('from', filters.from);
    if (filters.until) params.set('until', filters.until);
  }
  if (filters.sort) params.set('sort', apiSortToJspSort(filters.sort));
  return params;
}

/** true si no hay ningún filtro activo (para mostrar estado inicial/empty). */
export function isEmptyFilters(filters: SearchFilters): boolean {
  return Object.values(filters).every((v) => v === undefined || v === null || v === '');
}
