import { useQuery } from '@tanstack/react-query';
import { useSessionStore } from '../../../session/sessionStore';
import { pageCount } from '../../browse/hooks';
import { listReservations, type ReservationListQuery } from '../api';
import { jspReservationSortToApi } from '../reservationListSort';
import {
  OWNER_RESERVATIONS_PAGE_SIZE,
  type OwnerReservationFilters,
} from '../ownerReservationFilters';
import type { ReservationSummaryDto } from '../types';

function filtersToApiQuery(
  ownerId: string | number,
  filters: OwnerReservationFilters,
  pageIndex: number,
  carId?: string | number,
): ReservationListQuery {
  return {
    ownerId,
    carId,
    page: pageIndex + 1,
    pageSize: OWNER_RESERVATIONS_PAGE_SIZE,
    q: filters.ownerQ,
    status: filters.ownerStatus,
    category: filters.ownerCategory,
    transmission: filters.ownerTransmission,
    powertrain: filters.ownerPowertrain,
    priceMin: filters.ownerPriceMin,
    priceMax: filters.ownerPriceMax,
    rating: filters.ownerRating,
    sort: jspReservationSortToApi(filters.ownerSort),
  };
}

export function useOwnerReservationsPage(
  ownerId: string | number | null | undefined,
  filters: OwnerReservationFilters,
  pageIndex: number,
  carId?: string | number,
) {
  const ownedReservationsLink = useSessionStore(
    (s) => s.currentUser?.links?.['owned-reservations'],
  );
  return useQuery({
    queryKey: ['reservations', 'owner-page', ownedReservationsLink, ownerId, carId, filters, pageIndex],
    enabled: !!ownedReservationsLink && ownerId != null,
    queryFn: async () => {
      if (!ownedReservationsLink) throw new Error('reservations.list.missingLink');
      const res = await listReservations(
        filtersToApiQuery(ownerId as string | number, filters, pageIndex, carId),
        ownedReservationsLink,
      );
      return {
        items: (res.data ?? []) as ReservationSummaryDto[],
        total: res.page.total,
      };
    },
  });
}

export function ownerReservationsPageCount(total: number | undefined): number {
  return pageCount(total, OWNER_RESERVATIONS_PAGE_SIZE);
}
