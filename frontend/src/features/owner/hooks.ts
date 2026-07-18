// Helpers compartidos por las páginas OWNER.

import { useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useSessionStore } from '../../session/sessionStore';
import { resolveApiErrorMessage } from '../../api/apiErrorMessage';
import { pageCount } from '../browse/hooks';
import { listReservations } from '../reservations/api';
import type { ReservationSummaryDto } from '../reservations/types';
import { fetchOwnerCars, idFromUri } from './api';
import { pickCarReservationPreview } from './carReservationPreview';
import { jspSortToApiSort, MY_CARS_PAGE_SIZE, type MyCarsFilters } from './myCarsFilters';
import type { CarSummaryDto } from './types';
import type { UserDto } from '../../api/types';

/** id numérico del usuario logueado, derivado de su URN (currentUserUri). */
export function useCurrentUserId(): string | null {
  const uri = useSessionStore((s) => s.currentUserUri);
  return idFromUri(uri);
}

/** Un CBU cargado en el perfil (mismo criterio que `ownerHasValidCbu` del JSP: no vacío). */
export function hasCbu(user: UserDto | null | undefined): boolean {
  return !!user?.cbu && user.cbu.trim().length > 0;
}

/**
 * Traduce un error de la API a un mensaje i18n. El backend manda `message` con la
 * CLAVE i18n del error (p.ej. "user.profile.cbuInvalid", "car.publish.cbuRequired"),
 * mapeada en el catálogo global `error.byCode.*`; si hay match, se usa ese mensaje
 * específico. Si no, se tratan 403 (prerequisitos no cumplidos: identidad/cbu) y 409
 * con mensajes propios del área, y por último el `fallbackKey`.
 */
export function useApiErrorMessage(): (err: unknown, fallbackKey?: string) => string {
  const { t } = useTranslation();
  return useCallback(
    (err, fallbackKey = 'owner.errors.generic') => resolveApiErrorMessage(t, err, fallbackKey),
    [t],
  );
}

/** Página de "Mis autos" con filtros avanzados (paridad myCars.jsp) + paginación numerada. */
export function useMyCarsPage(
  filters: MyCarsFilters,
  pageIndex: number,
) {
  const carsLink = useSessionStore((s) => s.currentUser?.links?.cars);
  return useQuery({
    queryKey: ['owner', 'cars', 'page', carsLink, filters, pageIndex],
    enabled: !!carsLink,
    queryFn: async () => {
      const res = await fetchOwnerCars(carsLink as string, {
        page: pageIndex + 1,
        pageSize: MY_CARS_PAGE_SIZE,
        q: filters.q,
        status: filters.status,
        category: filters.category,
        transmission: filters.transmission,
        powertrain: filters.powertrain,
        priceMin: filters.priceMin,
        priceMax: filters.priceMax,
        rating: filters.rating,
        sort: jspSortToApiSort(filters.sort ?? 'date,desc'),
      });
      return {
        items: (res.data ?? []) as CarSummaryDto[],
        total: res.page.total,
      };
    },
  });
}

export function myCarsPageCount(total: number | undefined): number {
  return pageCount(total, MY_CARS_PAGE_SIZE);
}

/** Vista previa de reservas activas del auto (en curso o próximas) para el detalle owner. */
export function useCarReservationPreview(
  _ownerId: string | null | undefined,
  carId: string | null | undefined,
) {
  const ownedReservationsLink = useSessionStore(
    (s) => s.currentUser?.links?.['owned-reservations'],
  );
  return useQuery({
    queryKey: ['owner', 'car-reservations-preview', ownedReservationsLink, carId],
    enabled: !!ownedReservationsLink && carId != null,
    queryFn: async () => {
      if (!ownedReservationsLink) throw new Error('reservations.list.missingLink');
      const res = await listReservations(
        {
          carId: carId as string,
          status: ['pending', 'accepted', 'started'],
          page: 1,
          pageSize: 20,
        },
        ownedReservationsLink,
      );
      const items = (res.data ?? []) as ReservationSummaryDto[];
      return {
        preview: pickCarReservationPreview(items),
        total: res.page.total,
      };
    },
  });
}
