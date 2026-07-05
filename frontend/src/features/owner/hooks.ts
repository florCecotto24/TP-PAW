// Helpers compartidos por las páginas OWNER.

import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useSessionStore } from '../../session/sessionStore';
import { resolveApiErrorMessage } from '../../api/apiErrorMessage';
import { pageCount } from '../browse/hooks';
import { fetchOwnerCars, idFromUri } from './api';
import { jspSortToApiSort, MY_CARS_PAGE_SIZE, type MyCarsFilters } from './myCarsFilters';
import type { CarDto } from './types';
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
  return (err, fallbackKey = 'owner.errors.generic') => resolveApiErrorMessage(t, err, fallbackKey);
}

/** Página de "Mis autos" con filtros avanzados (paridad myCars.jsp) + paginación numerada. */
export function useMyCarsPage(
  ownerId: string | null | undefined,
  filters: MyCarsFilters,
  pageIndex: number,
) {
  return useQuery({
    queryKey: ['owner', 'cars', 'page', ownerId, filters, pageIndex],
    enabled: ownerId != null,
    queryFn: async () => {
      const res = await fetchOwnerCars(ownerId as string, {
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
        items: (res.data ?? []) as CarDto[],
        total: res.page.total,
      };
    },
  });
}

export function myCarsPageCount(total: number | undefined): number {
  return pageCount(total, MY_CARS_PAGE_SIZE);
}
