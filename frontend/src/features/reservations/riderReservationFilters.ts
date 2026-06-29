import { paths } from '../../routes/paths';
import {
  categoryFilterOptions,
  powertrainFilterOptions,
  ratingFilterOptions,
  transmissionFilterOptions,
} from '../browse/exploreSearch';
import { CAR_TYPES, POWERTRAINS, TRANSMISSIONS, type CarType, type Powertrain, type Transmission } from '../browse/types';
import type { ReservationStatus } from './types';

export const RIDER_RESERVATIONS_PAGE_SIZE = 8;

const RESERVATION_STATUSES: readonly ReservationStatus[] = [
  'pending',
  'accepted',
  'started',
  'cancelled',
  'cancelled_by_rider',
  'cancelled_by_owner',
  'cancelled_due_to_missing_payment_proof',
  'finished',
];

const JSP_SORTS = new Set([
  'date,desc',
  'date,asc',
  'price,asc',
  'price,desc',
  'rating,asc',
  'rating,desc',
]);

export interface RiderReservationFilters {
  q?: string;
  riderStatus?: ReservationStatus[];
  category?: CarType[];
  transmission?: Transmission[];
  powertrain?: Powertrain[];
  priceMin?: number;
  priceMax?: number;
  rating?: string[];
  sort?: string;
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

export function parseRiderReservationFilters(params: URLSearchParams): RiderReservationFilters {
  const q = params.get('q')?.trim();
  const sortRaw = params.get('sort')?.trim();
  const sort = sortRaw && JSP_SORTS.has(sortRaw) ? sortRaw : undefined;

  const filters: RiderReservationFilters = {
    q: q || undefined,
    riderStatus: inEnumList(readAll(params, 'riderStatus'), RESERVATION_STATUSES),
    category: inEnumList(readAll(params, 'category'), CAR_TYPES),
    transmission: inEnumList(readAll(params, 'transmission'), TRANSMISSIONS),
    powertrain: inEnumList(readAll(params, 'powertrain'), POWERTRAINS),
    priceMin: toNonNegativeNumber(params.get('priceMin')),
    priceMax: toNonNegativeNumber(params.get('priceMax')),
    rating: readAll(params, 'rating').filter((r) => ['3', '4', '5'].includes(r)),
    sort,
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

export function riderPageIndex(params: URLSearchParams): number {
  const raw = Number(params.get('riderPage') ?? '0');
  return Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0;
}

export function hasActiveRiderReservationFilters(params: URLSearchParams): boolean {
  const keys = [
    'q',
    'riderStatus',
    'category',
    'transmission',
    'powertrain',
    'priceMin',
    'priceMax',
    'rating',
  ];
  return keys.some((k) => params.has(k));
}

export function riderReservationsBasePath(params: URLSearchParams): string {
  const copy = new URLSearchParams(params);
  copy.delete('riderPage');
  copy.delete('sort');
  const qs = copy.toString();
  return qs ? `${paths.myReservations}?${qs}` : paths.myReservations;
}

export function filtersToRiderSearchParams(filters: RiderReservationFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('q', filters.q);
  filters.riderStatus?.forEach((s) => params.append('riderStatus', s));
  filters.category?.forEach((c) => params.append('category', c));
  filters.transmission?.forEach((t) => params.append('transmission', t));
  filters.powertrain?.forEach((p) => params.append('powertrain', p));
  if (filters.priceMin !== undefined) params.set('priceMin', String(filters.priceMin));
  if (filters.priceMax !== undefined) params.set('priceMax', String(filters.priceMax));
  filters.rating?.forEach((r) => params.append('rating', r));
  if (filters.sort) params.set('sort', filters.sort);
  return params;
}

export function reservationStatusFilterOptions(t: (key: string) => string) {
  return RESERVATION_STATUSES.map((key) => ({
    key,
    label: t(`enum.reservation.status.${key}`),
  }));
}

export function riderCarFilterOptions(t: (key: string) => string) {
  return {
    category: categoryFilterOptions(t),
    transmission: transmissionFilterOptions(t),
    powertrain: powertrainFilterOptions(t),
    rating: ratingFilterOptions(),
  };
}

export function showRiderFilterClear(params: URLSearchParams): boolean {
  const sort = params.get('sort');
  return hasActiveRiderReservationFilters(params) || Boolean(sort && sort !== 'date,desc');
}
