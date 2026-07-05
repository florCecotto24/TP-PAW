// Filtros avanzados de "Mis autos" (paridad con myCars.jsp): búsqueda de texto +
// dropdowns multi-select (estado/categoría/transmisión/motor/rating) + rango de
// precio + orden + paginación numerada. Mismo patrón que ownerReservationFilters.ts.

import { paths } from '../../routes/paths';
import {
  apiSortToJspSort,
  categoryFilterOptions,
  jspSortToApiSort,
  powertrainFilterOptions,
  ratingFilterOptions,
  transmissionFilterOptions,
} from '../browse/exploreSearch';
import { CAR_TYPES, POWERTRAINS, TRANSMISSIONS } from '../browse/types';
import type { CarType, Powertrain, Transmission } from '../browse/types';
import type { CarStatus } from './types';

export const MY_CARS_PAGE_SIZE = 8;

const CAR_STATUSES: readonly CarStatus[] = [
  'active',
  'paused',
  'admin_paused',
  'lack_doc',
  'unavailable',
  'deactivated',
];

export interface MyCarsFilters {
  q?: string;
  status?: CarStatus[];
  category?: CarType[];
  transmission?: Transmission[];
  powertrain?: Powertrain[];
  priceMin?: number;
  priceMax?: number;
  rating?: string[];
  sort?: string; // formato JSP: "date,desc" | "price,asc" | "rating,desc" | ...
}

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

function toNonNegativeNumber(value: string | null): number | undefined {
  if (value == null || value.trim() === '') return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

export function parseMyCarsFilters(params: URLSearchParams): MyCarsFilters {
  const q = params.get('q')?.trim();
  const sortRaw = params.get('sort')?.trim();

  const filters: MyCarsFilters = {
    q: q || undefined,
    status: inEnumList(readAll(params, 'listingStatus'), CAR_STATUSES),
    category: inEnumList(readAll(params, 'category'), CAR_TYPES),
    transmission: inEnumList(readAll(params, 'transmission'), TRANSMISSIONS),
    powertrain: inEnumList(readAll(params, 'powertrain'), POWERTRAINS),
    priceMin: toNonNegativeNumber(params.get('priceMin')),
    priceMax: toNonNegativeNumber(params.get('priceMax')),
    rating: readAll(params, 'rating').filter((r) => ['3', '4', '5'].includes(r)),
    sort: sortRaw || undefined,
  };

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

export function myCarsPageIndex(params: URLSearchParams): number {
  const raw = Number(params.get('page') ?? '0');
  return Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0;
}

export function hasActiveMyCarsFilters(params: URLSearchParams): boolean {
  return [
    'q',
    'listingStatus',
    'category',
    'transmission',
    'powertrain',
    'priceMin',
    'priceMax',
    'rating',
  ].some((k) => params.has(k));
}

export function myCarsBasePath(params: URLSearchParams): string {
  const copy = new URLSearchParams(params);
  copy.delete('page');
  copy.delete('sort');
  const qs = copy.toString();
  return qs ? `${paths.myCars}?${qs}` : paths.myCars;
}

export function filtersToMyCarsSearchParams(filters: MyCarsFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('q', filters.q);
  filters.status?.forEach((s) => params.append('listingStatus', s));
  filters.category?.forEach((c) => params.append('category', c));
  filters.transmission?.forEach((t) => params.append('transmission', t));
  filters.powertrain?.forEach((p) => params.append('powertrain', p));
  if (filters.priceMin !== undefined) params.set('priceMin', String(filters.priceMin));
  if (filters.priceMax !== undefined) params.set('priceMax', String(filters.priceMax));
  filters.rating?.forEach((r) => params.append('rating', r));
  if (filters.sort) params.set('sort', filters.sort);
  return params;
}

export function myCarsFilterOptions(t: (key: string) => string) {
  return {
    category: categoryFilterOptions(t),
    transmission: transmissionFilterOptions(t),
    powertrain: powertrainFilterOptions(t),
    rating: ratingFilterOptions(),
    status: CAR_STATUSES.map((key) => ({ key, label: t(`owner.enums.status.${key}`) })),
  };
}

export function showMyCarsFilterClear(params: URLSearchParams): boolean {
  const sort = params.get('sort');
  return hasActiveMyCarsFilters(params) || Boolean(sort && sort !== 'date,desc');
}

export { apiSortToJspSort, jspSortToApiSort };
