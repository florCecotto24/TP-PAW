import type { CarSort } from './types';
import type { ExploreFilterOption } from '../../components/ryden';
import { paths } from '../../routes/paths';
import { CAR_TYPES, POWERTRAINS, TRANSMISSIONS } from './types';

export const SEARCH_PAGE_SIZE = 12;

/** Mapeo sort API REST ↔ sort del tag JSP {@code sortBar}. */
export function apiSortToJspSort(sort?: CarSort): string {
  switch (sort) {
    case 'price_asc':
      return 'price,asc';
    case 'price_desc':
      return 'price,desc';
    case 'rating_desc':
      return 'rating,desc';
    case 'recent':
    default:
      return 'date,desc';
  }
}

export function jspSortToApiSort(jsp: string): CarSort {
  switch (jsp) {
    case 'price,asc':
      return 'price_asc';
    case 'price,desc':
      return 'price_desc';
    case 'rating,desc':
      return 'rating_desc';
    case 'date,asc':
    case 'rating,asc':
    case 'date,desc':
    default:
      return 'recent';
  }
}

export function buildFilterOptions(
  t: (key: string) => string,
  prefix: 'browse.carType' | 'browse.transmission' | 'browse.powertrain',
  values: readonly string[],
): ExploreFilterOption[] {
  return values.map((key) => ({
    key,
    label: t(`${prefix}.${key}`),
  }));
}

export function categoryFilterOptions(t: (key: string) => string): ExploreFilterOption[] {
  return buildFilterOptions(t, 'browse.carType', CAR_TYPES);
}

export function transmissionFilterOptions(t: (key: string) => string): ExploreFilterOption[] {
  return buildFilterOptions(t, 'browse.transmission', TRANSMISSIONS);
}

export function powertrainFilterOptions(t: (key: string) => string): ExploreFilterOption[] {
  return buildFilterOptions(t, 'browse.powertrain', POWERTRAINS);
}

export function ratingFilterOptions(): ExploreFilterOption[] {
  return ['5', '4', '3'].map((key) => ({ key, label: `${key}+` }));
}

export function priceMarketFilterOptions(t: (key: string) => string): ExploreFilterOption[] {
  return [
    {
      key: 'below_market',
      label: t('search.filter.priceMarket.below'),
    },
    {
      key: 'at_market',
      label: t('search.filter.priceMarket.at'),
    },
    {
      key: 'above_market',
      label: t('search.filter.priceMarket.above'),
    },
  ];
}

export function hasActiveSearchFilters(params: URLSearchParams): boolean {
  const keys = [
    'query',
    'q',
    'from',
    'until',
    'neighborhoodId',
    'category',
    'transmission',
    'powertrain',
    'priceMin',
    'priceMax',
    'priceMarket',
    'rating',
    'flexible',
    'flexMonth',
    'flexDays',
  ];
  return keys.some((k) => params.has(k));
}

export function searchBasePath(params: URLSearchParams): string {
  const copy = new URLSearchParams(params);
  copy.delete('page');
  copy.delete('sort');
  return withQuery(paths.search, copy);
}

function withQuery(basePath: string, params: URLSearchParams): string {
  const qs = params.toString();
  return qs ? `${basePath}?${qs}` : basePath;
}
