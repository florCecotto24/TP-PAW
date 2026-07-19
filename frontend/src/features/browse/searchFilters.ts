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

export type PriceMarketFilter = 'below_market' | 'at_market' | 'above_market';

export interface SearchFilters {
  q?: string;
  category?: CarType[];
  transmission?: Transmission[];
  powertrain?: Powertrain[];
  priceMin?: number;
  priceMax?: number;
  /** Posición vs mercado del mismo modelo (mismas bandas que el badge de la card). */
  priceMarket?: PriceMarketFilter[];
  /** UI keys {@code 3}/{@code 4}/{@code 5} (= piso 3+/4+/5+), repetibles. */
  rating?: string[];
  neighborhoodIds?: number[];
  from?: string; // YYYY-MM-DD
  until?: string; // YYYY-MM-DD
  flexible?: boolean;
  flexMonth?: string; // YYYY-MM
  flexDays?: number;
  sort?: CarSort;
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const FLEX_MONTH = /^\d{4}-\d{2}$/;
const PRICE_MARKET_VALUES = ['below_market', 'at_market', 'above_market'] as const;
const RATING_UI_KEYS = ['3', '4', '5'] as const;

function readAll(params: URLSearchParams, key: string): string[] {
  return params.getAll(key).map((v) => v.trim()).filter(Boolean);
}

function inEnumList<T extends string>(values: string[], allowed: readonly T[]): T[] {
  const out: T[] = [];
  for (const value of values) {
    if ((allowed as readonly string[]).includes(value)) {
      out.push(value as T);
    }
  }
  return out;
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

function toPriceMarketList(values: string[]): PriceMarketFilter[] {
  return inEnumList(values, PRICE_MARKET_VALUES);
}

function toNeighborhoodIds(params: URLSearchParams): number[] {
  const out: number[] = [];
  for (const raw of readAll(params, 'neighborhoodId')) {
    const id = toPositiveInt(raw);
    if (id !== undefined && !out.includes(id)) {
      out.push(id);
    }
  }
  return out;
}

/**
 * Lee filtros desde los search params de la URL. Tolerante: valores inválidos o
 * fuera de rango se descartan (no rompen la pantalla). Strings vacíos se omiten.
 * Params repetibles (`powertrain=…&powertrain=…`) se conservan todos.
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

  const category = inEnumList(readAll(params, 'category'), CAR_TYPES);
  const transmission = inEnumList(readAll(params, 'transmission'), TRANSMISSIONS);
  const powertrain = inEnumList(readAll(params, 'powertrain'), POWERTRAINS);
  const priceMarket = toPriceMarketList(readAll(params, 'priceMarket'));
  const rating = inEnumList(readAll(params, 'rating'), RATING_UI_KEYS);
  const neighborhoodIds = toNeighborhoodIds(params);

  const filters: SearchFilters = {
    q: q ? q : undefined,
    category: category.length ? category : undefined,
    transmission: transmission.length ? transmission : undefined,
    powertrain: powertrain.length ? powertrain : undefined,
    priceMin: toNonNegativeNumber(params.get('priceMin')),
    priceMax: toNonNegativeNumber(params.get('priceMax')),
    priceMarket: priceMarket.length ? priceMarket : undefined,
    rating: rating.length ? rating : undefined,
    neighborhoodIds: neighborhoodIds.length ? neighborhoodIds : undefined,
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
 * Serializa filtros para la API REST (`q`, sort API, arrays → keys repetidas).
 */
export function filtersToApiParams(
  filters: SearchFilters,
): Record<string, string | string[] | number | boolean> {
  const out: Record<string, string | string[] | number | boolean> = {};
  if (filters.q?.trim()) out.q = filters.q.trim();
  if (filters.category?.length) out.category = [...filters.category];
  if (filters.transmission?.length) out.transmission = [...filters.transmission];
  if (filters.powertrain?.length) out.powertrain = [...filters.powertrain];
  if (filters.priceMin !== undefined) out.priceMin = filters.priceMin;
  if (filters.priceMax !== undefined) out.priceMax = filters.priceMax;
  if (filters.priceMarket?.length) out.priceMarket = [...filters.priceMarket];
  if (filters.rating?.length) out.rating = [...filters.rating];
  if (filters.neighborhoodIds?.length) {
    out.neighborhoodId = filters.neighborhoodIds.map(String);
  }
  if (filters.flexible) {
    out.flexible = true;
    if (filters.flexMonth) out.flexMonth = filters.flexMonth;
    if (filters.flexDays !== undefined) out.flexDays = filters.flexDays;
  } else {
    if (filters.from) out.from = filters.from;
    if (filters.until) out.until = filters.until;
  }
  if (filters.sort) out.sort = filters.sort;
  return out;
}

/**
 * Serializa filtros a URL (paridad JSP: `query`, sort `date,desc`, params repetidos).
 */
export function filtersToSearchParams(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('query', filters.q);
  filters.category?.forEach((c) => params.append('category', c));
  filters.transmission?.forEach((t) => params.append('transmission', t));
  filters.powertrain?.forEach((p) => params.append('powertrain', p));
  if (filters.priceMin !== undefined) params.set('priceMin', String(filters.priceMin));
  if (filters.priceMax !== undefined) params.set('priceMax', String(filters.priceMax));
  filters.priceMarket?.forEach((p) => params.append('priceMarket', p));
  filters.rating?.forEach((r) => params.append('rating', r));
  filters.neighborhoodIds?.forEach((id) => params.append('neighborhoodId', String(id)));
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
  return Object.values(filters).every(
    (v) => v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0),
  );
}
