import type { ReservationListQuery } from './api';

const DEFAULT_JSP_SORT = 'date,desc';

export function jspReservationSortToApi(sort?: string): ReservationListQuery['sort'] {
  switch (sort) {
    case 'date,asc':
      return 'start_date';
    case 'price,asc':
      return 'price_asc';
    case 'price,desc':
      return 'price_desc';
    case 'date,desc':
    default:
      return 'recent';
  }
}

export function apiReservationSortToJsp(sort?: ReservationListQuery['sort']): string {
  switch (sort) {
    case 'start_date':
      return 'date,asc';
    case 'price_asc':
      return 'price,asc';
    case 'price_desc':
      return 'price,desc';
    case 'recent':
    default:
      return DEFAULT_JSP_SORT;
  }
}

export function currentJspReservationSort(sort?: string): string {
  return sort && sort.trim() !== '' ? sort : DEFAULT_JSP_SORT;
}
