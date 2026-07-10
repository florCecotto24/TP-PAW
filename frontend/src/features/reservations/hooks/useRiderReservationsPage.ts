import { useQuery } from '@tanstack/react-query';
import { pageCount } from '../../browse/hooks';
import { listReservations, type ReservationListQuery } from '../api';
import { jspReservationSortToApi } from '../reservationListSort';
import {
  RIDER_RESERVATIONS_PAGE_SIZE,
  type RiderReservationFilters,
} from '../riderReservationFilters';
import type { ReservationSummaryDto } from '../types';

function filtersToApiQuery(
  riderId: string | number,
  filters: RiderReservationFilters,
  pageIndex: number,
): ReservationListQuery {
  return {
    riderId,
    page: pageIndex + 1,
    pageSize: RIDER_RESERVATIONS_PAGE_SIZE,
    q: filters.q,
    riderStatus: filters.riderStatus,
    category: filters.category,
    transmission: filters.transmission,
    powertrain: filters.powertrain,
    priceMin: filters.priceMin,
    priceMax: filters.priceMax,
    rating: filters.rating,
    sort: jspReservationSortToApi(filters.sort),
  };
}

export function useRiderReservationsPage(
  riderId: string | number | null | undefined,
  filters: RiderReservationFilters,
  pageIndex: number,
) {
  return useQuery({
    queryKey: ['reservations', 'rider-page', riderId, filters, pageIndex],
    enabled: riderId != null,
    queryFn: async () => {
      const res = await listReservations(filtersToApiQuery(riderId as string | number, filters, pageIndex));
      return {
        items: (res.data ?? []) as ReservationSummaryDto[],
        total: res.page.total,
      };
    },
  });
}

export function riderReservationsPageCount(total: number | undefined): number {
  return pageCount(total, RIDER_RESERVATIONS_PAGE_SIZE);
}
