// Hooks de fetching de BROWSE (react-query sobre el sessionClient hypermedia).
//
// Convenciones del cliente (ver ../../api/client):
//   - GET de colección: accept MediaTypes.car -> res.data es el ARRAY; la
//     paginación vive en res.page (NO en el body).
//   - próxima página = navegar res.page.next (otra request a esa URL absoluta).
//   - hipervínculos: se navega car.links.* (follow), no se arman URLs.

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
} from '@tanstack/react-query';
import { sessionClient, useSessionStore } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import type { ApiResponse } from '../../api/client';
import type { UserDto } from '../../api/types';
import { lastPathSegment } from '../../api/uri';
import { filtersToParams, type SearchFilters } from './searchFilters';
import type {
  AvailabilityDto,
  CarDto,
  NeighborhoodDto,
  PictureDto,
  ReviewDto,
} from './types';

const CARS_PATH = '/cars';

// --- Home: dos carruseles (8 autos c/u) --------------------------------------
function useCarCarousel(sort: string) {
  return useQuery({
    queryKey: ['browse', 'cars', 'carousel', sort],
    queryFn: async () => {
      const res = await sessionClient.get<CarDto[]>(CARS_PATH, {
        accept: MediaTypes.car,
        query: { sort, pageSize: 8 },
      });
      // 204 -> data undefined; normalizo a array vacío.
      return res.data ?? [];
    },
  });
}

export function useCheapestCars() {
  return useCarCarousel('price_asc');
}

export function useRecentCars() {
  return useCarCarousel('recent');
}

// --- Búsqueda paginada (navega res.page.next/prev) ---------------------------
// pageParam = URL absoluta de la página (null = primera, usa CARS_PATH+filtros).
export function useSearchCars(filters: SearchFilters, pageSize = 12) {
  return useInfiniteQuery({
    queryKey: ['browse', 'cars', 'search', filtersToParams(filters), pageSize],
    initialPageParam: null as string | null,
    queryFn: async ({ pageParam }) => {
      if (pageParam) {
        // Página subsiguiente: navego el link tal cual lo dio la API.
        const res = await sessionClient.follow<CarDto[]>(pageParam, { accept: MediaTypes.car });
        return { items: res.data ?? [], page: res.page };
      }
      const res = await sessionClient.get<CarDto[]>(CARS_PATH, {
        accept: MediaTypes.car,
        query: { ...filtersToParams(filters), pageSize },
      });
      return { items: res.data ?? [], page: res.page };
    },
    getNextPageParam: (last) => last.page.next ?? undefined,
    getPreviousPageParam: (first) => first.page.prev ?? undefined,
  });
}

/** Aplana las páginas de useSearchCars en una sola lista de autos. */
export function flattenCars(
  data: InfiniteData<{ items: CarDto[] }> | undefined,
): CarDto[] {
  return data?.pages.flatMap((p) => p.items) ?? [];
}

/** Total de resultados (X-Total-Count de la primera página). */
export function searchTotal(
  data: InfiniteData<{ page: { total?: number } }> | undefined,
): number | undefined {
  return data?.pages[0]?.page.total;
}

// --- Detalle de auto ---------------------------------------------------------
export function useCar(id: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'car', id],
    enabled: !!id,
    queryFn: async () => {
      const res = await sessionClient.get<CarDto>(`${CARS_PATH}/${id}`, {
        accept: MediaTypes.car,
      });
      return res.data;
    },
  });
}

/** Galería: navega car.links.pictures. Los bytes están en cada pic.links.self. */
export function useCarPictures(picturesLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'pictures', picturesLink],
    enabled: !!picturesLink,
    queryFn: async () => {
      const res = await sessionClient.follow<PictureDto[]>(picturesLink as string, {
        accept: MediaTypes.picture,
      });
      return (res.data ?? []).sort((a, b) => a.displayOrder - b.displayOrder);
    },
  });
}

/** Dueño del auto: navega car.links.owner para el bloque de contacto/perfil. */
export function useCarOwner(ownerLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'owner', ownerLink],
    enabled: !!ownerLink,
    queryFn: async () => {
      const res = await sessionClient.follow<UserDto>(ownerLink as string, {
        accept: MediaTypes.user,
      });
      return res.data;
    },
  });
}

/** Disponibilidad: navega car.links.availabilities. */
export function useCarAvailabilities(availabilitiesLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'availabilities', availabilitiesLink],
    enabled: !!availabilitiesLink,
    queryFn: async () => {
      const res = await sessionClient.follow<AvailabilityDto[]>(availabilitiesLink as string, {
        accept: MediaTypes.availability,
      });
      return res.data ?? [];
    },
  });
}

/** Reseñas paginadas: navega car.links.reviews y luego res.page.next. */
export function useCarReviews(reviewsLink: string | undefined) {
  return useInfiniteQuery({
    queryKey: ['browse', 'reviews', reviewsLink],
    enabled: !!reviewsLink,
    initialPageParam: reviewsLink ?? null,
    queryFn: async ({ pageParam }) => {
      const res = await sessionClient.follow<ReviewDto[]>(pageParam as string, {
        accept: MediaTypes.review,
      });
      return { items: res.data ?? [], page: res.page };
    },
    getNextPageParam: (last) => last.page.next ?? undefined,
  });
}

/** Total de reseñas (X-Total-Count de la primera página de reviews, si vino). */
export function reviewsTotal(
  data: InfiniteData<{ page: { total?: number } }> | undefined,
): number | undefined {
  return data?.pages[0]?.page.total;
}

// --- Barrios para el filtro de búsqueda --------------------------------------
export function useNeighborhoods() {
  return useQuery({
    queryKey: ['browse', 'neighborhoods'],
    staleTime: 60 * 60 * 1000, // catálogo casi estático
    queryFn: async () => {
      const res = await sessionClient.get<NeighborhoodDto[]>('/neighborhoods', {
        accept: MediaTypes.neighborhood,
      });
      return res.data ?? [];
    },
  });
}

// --- Favoritos (idempotente: PUT agrega, DELETE quita) -----------------------
// La colección de favoritos del usuario es currentUser.links.favorites; un
// favorito individual es `${favorites}/{carId}`. carId se obtiene del car.links.self
// (último segmento de la URN del auto). Hipermedia: no se arma /users/{id}/...

/**
 * ¿Está este auto en los favoritos del usuario logueado? El back no expone un
 * GET de membresía individual (`/favorites/{carId}` solo soporta PUT/DELETE),
 * así que navegamos la colección paginada currentUser.links.favorites (siguiendo
 * los links `next` del header) y buscamos el self del auto. La query se registra
 * bajo ['browse','favorites', carSelf] para que useToggleFavorite la invalide.
 */
export function useIsFavorite(carSelf: string | undefined) {
  const favoritesLink = useSessionStore((s) => s.currentUser?.links.favorites);
  return useQuery({
    queryKey: ['browse', 'favorites', carSelf],
    enabled: !!carSelf && !!favoritesLink,
    queryFn: async () => {
      // Comparo por id (último segmento) y no por `===`: el self del listado y el
      // del detalle pueden diferir en absoluto/relativo (mismo criterio que isOwner).
      const targetId = lastPathSegment(carSelf as string);
      let pageLink: string | undefined = favoritesLink as string;
      while (pageLink) {
        const res: ApiResponse<CarDto[]> = await sessionClient.follow<CarDto[]>(pageLink, {
          accept: MediaTypes.car,
        });
        if ((res.data ?? []).some((c: CarDto) => lastPathSegment(c.links.self) === targetId)) return true;
        pageLink = res.page.next ?? undefined;
      }
      return false;
    },
  });
}

export function useToggleFavorite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ car, makeFavorite }: { car: CarDto; makeFavorite: boolean }) => {
      const favoritesLink = useSessionStore.getState().currentUser?.links.favorites;
      if (!favoritesLink) throw new Error('browse.favorite.noSession');
      const carId = lastPathSegment(car.links.self);
      const target = `${stripTrailingSlash(favoritesLink)}/${carId}`;
      const res: ApiResponse<unknown> = makeFavorite
        ? await sessionClient.put(target)
        : await sessionClient.del(target);
      return res;
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['browse', 'favorites'] });
    },
  });
}

function stripTrailingSlash(s: string): string {
  return s.endsWith('/') ? s.slice(0, -1) : s;
}
