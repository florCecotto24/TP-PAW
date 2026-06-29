import { paths, ownerReservationsCar } from '../../routes/paths';
import {
  categoryFilterOptions,
  powertrainFilterOptions,
  ratingFilterOptions,
  transmissionFilterOptions,
} from '../browse/exploreSearch';
import { CAR_TYPES, POWERTRAINS, TRANSMISSIONS, type CarType, type Powertrain, type Transmission } from '../browse/types';
import type { ReservationStatus } from './types';
import { reservationStatusFilterOptions } from './riderReservationFilters';

export const OWNER_RESERVATIONS_PAGE_SIZE = 8;

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

export interface OwnerReservationFilters {
  ownerQ?: string;
  ownerStatus?: ReservationStatus[];
  ownerCategory?: CarType[];
  ownerTransmission?: Transmission[];
  ownerPowertrain?: Powertrain[];
  ownerPriceMin?: number;
  ownerPriceMax?: number;
  ownerRating?: string[];
  ownerSort?: string;
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

export function parseOwnerReservationFilters(params: URLSearchParams): OwnerReservationFilters {
  const ownerQ = params.get('ownerQ')?.trim();
  const sortRaw = params.get('ownerSort')?.trim();
  const ownerSort = sortRaw && JSP_SORTS.has(sortRaw) ? sortRaw : undefined;

  const filters: OwnerReservationFilters = {
    ownerQ: ownerQ || undefined,
    ownerStatus: inEnumList(readAll(params, 'ownerStatus'), RESERVATION_STATUSES),
    ownerCategory: inEnumList(readAll(params, 'ownerCategory'), CAR_TYPES),
    ownerTransmission: inEnumList(readAll(params, 'ownerTransmission'), TRANSMISSIONS),
    ownerPowertrain: inEnumList(readAll(params, 'ownerPowertrain'), POWERTRAINS),
    ownerPriceMin: toNonNegativeNumber(params.get('ownerPriceMin')),
    ownerPriceMax: toNonNegativeNumber(params.get('ownerPriceMax')),
    ownerRating: readAll(params, 'ownerRating').filter((r) => ['3', '4', '5'].includes(r)),
    ownerSort,
  };

  if (
    filters.ownerPriceMin !== undefined &&
    filters.ownerPriceMax !== undefined &&
    filters.ownerPriceMin > filters.ownerPriceMax
  ) {
    delete filters.ownerPriceMin;
    delete filters.ownerPriceMax;
  }

  return filters;
}

export function ownerPageIndex(params: URLSearchParams): number {
  const raw = Number(params.get('page') ?? '0');
  return Number.isFinite(raw) && raw >= 0 ? Math.floor(raw) : 0;
}

export function hasActiveOwnerReservationFilters(
  params: URLSearchParams,
  scopedToCar: boolean,
): boolean {
  const keys = scopedToCar
    ? ['ownerStatus', 'ownerPriceMin', 'ownerPriceMax', 'ownerRating']
    : [
        'ownerQ',
        'ownerStatus',
        'ownerCategory',
        'ownerTransmission',
        'ownerPowertrain',
        'ownerPriceMin',
        'ownerPriceMax',
        'ownerRating',
      ];
  return keys.some((k) => params.has(k));
}

export function ownerReservationsBasePath(params: URLSearchParams, carId?: string | null): string {
  const copy = new URLSearchParams(params);
  copy.delete('page');
  copy.delete('ownerSort');
  const qs = copy.toString();
  const base = carId ? ownerReservationsCar(carId) : paths.ownerReservations;
  return qs ? `${base}?${qs}` : base;
}

export function filtersToOwnerSearchParams(filters: OwnerReservationFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.ownerQ) params.set('ownerQ', filters.ownerQ);
  filters.ownerStatus?.forEach((s) => params.append('ownerStatus', s));
  filters.ownerCategory?.forEach((c) => params.append('ownerCategory', c));
  filters.ownerTransmission?.forEach((t) => params.append('ownerTransmission', t));
  filters.ownerPowertrain?.forEach((p) => params.append('ownerPowertrain', p));
  if (filters.ownerPriceMin !== undefined) params.set('ownerPriceMin', String(filters.ownerPriceMin));
  if (filters.ownerPriceMax !== undefined) params.set('ownerPriceMax', String(filters.ownerPriceMax));
  filters.ownerRating?.forEach((r) => params.append('ownerRating', r));
  if (filters.ownerSort) params.set('ownerSort', filters.ownerSort);
  return params;
}

export function ownerCarFilterOptions(t: (key: string) => string) {
  return {
    category: categoryFilterOptions(t),
    transmission: transmissionFilterOptions(t),
    powertrain: powertrainFilterOptions(t),
    rating: ratingFilterOptions(),
    status: reservationStatusFilterOptions(t),
  };
}

export function showOwnerFilterClear(params: URLSearchParams, scopedToCar: boolean): boolean {
  const sort = params.get('ownerSort');
  return (
    hasActiveOwnerReservationFilters(params, scopedToCar) ||
    Boolean(sort && sort !== 'date,desc')
  );
}

export { reservationStatusFilterOptions };
