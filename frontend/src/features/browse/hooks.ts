// Hooks de fetching de BROWSE (react-query sobre el sessionClient hypermedia).
//
// Convenciones del cliente (ver ../../api/client):
//   - GET de colección: accept MediaTypes.carSummary -> res.data es el ARRAY; la
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
import { getCollectionPath } from '../../api/apiDiscovery';
import { MediaTypes } from '../../api/mediaTypes';
import { ApiError, followAllPages, followLinkCollection, getLinkCollectionPage, type ApiResponse } from '../../api/client';
import type { UserDto, Links } from '../../api/types';
import { lastPathSegment } from '../../api/uri';
import { filtersToApiParams, type SearchFilters } from './searchFilters';
import { isCarFavoritable } from './carCardAdapter';
import type {
  AvailabilityDto,
  CarDto,
  CarSummaryDto,
  NeighborhoodDto,
  PictureDto,
  ReviewDto,
} from './types';

const CARS_PATH = () => getCollectionPath('cars');

function stripTrailingSlash(s: string): string {
  return s.endsWith('/') ? s.slice(0, -1) : s;
}

export const HOME_CAROUSEL_PAGE_SIZE = 8;

// --- Home: two carousels (8 cars each, 0-based page index in URL) ----------------
function useCarCarousel(sort: string, pageIndex: number) {
  return useQuery({
    queryKey: ['browse', 'cars', 'carousel', sort, pageIndex],
    queryFn: async () => {
      const res = await sessionClient.get<CarSummaryDto[]>(CARS_PATH(), {
        accept: MediaTypes.carSummary,
        query: { sort, page: pageIndex + 1, pageSize: HOME_CAROUSEL_PAGE_SIZE },
      });
      return {
        items: res.data ?? [],
        page: res.page,
      };
    },
  });
}

export function useCheapestCars(pageIndex = 0) {
  return useCarCarousel('price_asc', pageIndex);
}

export function useRecentCars(pageIndex = 0) {
  return useCarCarousel('recent', pageIndex);
}

// --- Búsqueda paginada (navega res.page.next/prev) ---------------------------
// pageParam = URL absoluta de la página (null = primera, usa CARS_PATH+filtros).
export function useSearchCars(filters: SearchFilters, pageSize = 12) {
  return useInfiniteQuery({
    queryKey: ['browse', 'cars', 'search', filtersToApiParams(filters), pageSize],
    initialPageParam: null as string | null,
    queryFn: async ({ pageParam }) => {
      if (pageParam) {
        // Página subsiguiente: navego el link tal cual lo dio la API.
        const res = await sessionClient.follow<CarSummaryDto[]>(pageParam, { accept: MediaTypes.carSummary });
        return { items: res.data ?? [], page: res.page };
      }
      const res = await sessionClient.get<CarSummaryDto[]>(CARS_PATH(), {
        accept: MediaTypes.carSummary,
        query: { ...filtersToApiParams(filters), pageSize },
      });
      return { items: res.data ?? [], page: res.page };
    },
    getNextPageParam: (last) => last.page.next ?? undefined,
    getPreviousPageParam: (first) => first.page.prev ?? undefined,
  });
}

/** Aplana las páginas de useSearchCars en una sola lista de autos. */
export function flattenCars(
  data: InfiniteData<{ items: CarSummaryDto[] }> | undefined,
): CarSummaryDto[] {
  return data?.pages.flatMap((p) => p.items) ?? [];
}

/** Total de resultados (X-Total-Count de la primera página). */
export function searchTotal(
  data: InfiniteData<{ page: { total?: number } }> | undefined,
): number | undefined {
  return data?.pages[0]?.page.total;
}

const SEARCH_PAGE_SIZE = 12;

/** Búsqueda paginada (una página) — alineada con search.jsp + ryden:pagination. */
export function useSearchCarsPage(filters: SearchFilters, page: number, pageSize = SEARCH_PAGE_SIZE) {
  return useQuery({
    queryKey: ['browse', 'cars', 'search-page', filtersToApiParams(filters), page, pageSize],
    queryFn: async () => {
      const res = await sessionClient.get<CarSummaryDto[]>(CARS_PATH(), {
        accept: MediaTypes.carSummary,
        query: { ...filtersToApiParams(filters), page, pageSize },
      });
      return { items: res.data ?? [], page: res.page };
    },
  });
}

export function pageCount(total: number | undefined, pageSize: number): number {
  if (total == null || total <= 0) return 0;
  return Math.ceil(total / pageSize);
}

// --- Detalle de auto ---------------------------------------------------------
export function useCar(id: string | undefined, carSelf?: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'car', carSelf ?? id],
    enabled: !!(carSelf || id),
    queryFn: async () => {
      if (carSelf) {
        const res = await sessionClient.follow<CarDto>(carSelf, { accept: MediaTypes.car });
        return res.data;
      }
      const res = await sessionClient.get<CarDto>(`${CARS_PATH()}/${id}`, {
        accept: MediaTypes.car,
      });
      return res.data;
    },
  });
}

/** Galería: navega car.links.pictures y sigue res.page.next si hay más de una página. */
export function useCarPictures(picturesLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'pictures', picturesLink],
    enabled: !!picturesLink,
    queryFn: async () => {
      const res = await followAllPages<PictureDto>(sessionClient, picturesLink as string, {
        accept: MediaTypes.picture,
      });
      return (res.data ?? []).sort((a, b) => a.displayOrder - b.displayOrder);
    },
  });
}

/**
 * Dueño del auto: navega car.links.owner.
 * Con {@code privateView} (solo admin) pide UserPrivateDto para leer rol/blocked
 * y poder ocultar acciones de moderación sobre autos de otros admins.
 */
export function useCarOwner(ownerLink: string | undefined, privateView = false) {
  const accept = privateView ? MediaTypes.userPrivate : MediaTypes.user;
  return useQuery({
    queryKey: ['browse', 'owner', ownerLink, privateView ? 'private' : 'public'],
    enabled: !!ownerLink,
    queryFn: async () => {
      const res = await sessionClient.follow<UserDto>(ownerLink as string, { accept });
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
      const collectionRes = await sessionClient.follow<Links[]>(pageParam as string, {
        accept: MediaTypes.reviewLinks,
      });
      const items = await followLinkCollection<ReviewDto>(sessionClient, collectionRes, MediaTypes.review);
      return { items, page: collectionRes.page };
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
      const res = await sessionClient.get<NeighborhoodDto[]>(getCollectionPath('neighborhoods'), {
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

async function loadFavoritedCarIds(favoritesLink: string): Promise<Set<string>> {
  const ids = new Set<string>();
  let pageLink: string | undefined = favoritesLink;
  while (pageLink) {
    const res: ApiResponse<Links[]> = await sessionClient.follow<Links[]>(pageLink, {
      accept: MediaTypes.userFavorites,
    });
    for (const link of res.data ?? []) {
      const id = lastPathSegment(link.self);
      if (id) ids.add(id);
    }
    pageLink = res.page.next ?? undefined;
  }
  return ids;
}

/** IDs de autos favoritos del usuario logueado (para grids de browse). */
export function useFavoritedCarIds() {
  const favoritesLink = useSessionStore((s) => s.currentUser?.links?.favorites);
  const userSelf = useSessionStore((s) => s.currentUser?.links?.self);
  return useQuery({
    queryKey: ['browse', 'favorites', 'ids', userSelf],
    enabled: !!favoritesLink && !!userSelf,
    queryFn: async () => {
      try {
        return await loadFavoritedCarIds(favoritesLink as string);
      } catch (err) {
        // Sesión vencida sin refresh válido: no romper la grilla ni spamear la consola.
        if (err instanceof ApiError && err.status === 401) {
          return new Set<string>();
        }
        throw err;
      }
    },
  });
}

/**
 * ¿Está este auto en favoritos? Usa {@code GET …/favorites/{carId}} (204 / 404).
 * No se puede inferir solo de la colección listada: el listado oculta estados
 * fuera de active/paused (p.ej. admin_paused), así el corazón del detalle
 * fallaba al favoritar autos abiertos desde el panel admin.
 */
export function useIsFavorite(carSelf: string | undefined) {
  const favoritesLink = useSessionStore((s) => s.currentUser?.links?.favorites);
  const userSelf = useSessionStore((s) => s.currentUser?.links?.self);
  return useQuery({
    queryKey: ['browse', 'favorites', userSelf, carSelf],
    enabled: !!carSelf && !!favoritesLink && !!userSelf,
    queryFn: async () => {
      const carId = lastPathSegment(carSelf as string);
      if (!carId) return false;
      const target = `${stripTrailingSlash(favoritesLink as string)}/${carId}`;
      try {
        await sessionClient.get(target);
        return true;
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) return false;
        throw err;
      }
    },
  });
}

/** Estado del corazón en tarjetas de browse (home, búsqueda, similares, perfil público). */
export function useBrowseCarFavorite(car: CarSummaryDto) {
  const isLoggedIn = useSessionStore((s) => s.status === 'authenticated');
  const userSelf = useSessionStore((s) => s.currentUser?.links?.self);
  const favoritable = isCarFavoritable(car, isLoggedIn, userSelf);
  const carId = lastPathSegment(car.links?.self ?? '');
  const favIdsQuery = useFavoritedCarIds();
  const favorited =
    favoritable && carId !== '' && (favIdsQuery.data?.has(carId) ?? false);
  const toggleFavorite = useToggleFavorite();

  return {
    favoritable,
    favorited,
    favoriteBusy: toggleFavorite.isPending || favIdsQuery.isLoading,
    onToggleFavorite: () => {
      void toggleFavorite.mutateAsync({ car, makeFavorite: !favorited });
    },
  };
}

export function useToggleFavorite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ car, makeFavorite }: { car: CarSummaryDto; makeFavorite: boolean }) => {
      const favoritesLink = useSessionStore.getState().currentUser?.links?.favorites;
      if (!favoritesLink) throw new Error('browse.favorite.noSession');
      const carId = lastPathSegment(car.links?.self ?? '');
      const target = `${stripTrailingSlash(favoritesLink)}/${carId}`;
      const res: ApiResponse<unknown> = makeFavorite
        ? await sessionClient.put(target)
        : await sessionClient.del(target);
      return res;
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['browse', 'favorites'] });
      void qc.invalidateQueries({ queryKey: ['profile', 'favorites'] });
    },
  });
}

/** Public reviews for car detail (one page, 1-based API page param). */
export const CAR_REVIEWS_PAGE_SIZE = 6;

/**
 * Página de reseñas del auto siguiendo {@code car.links.reviews} (canónico
 * {@code /reviews?carId=…}), no una URI armada a mano.
 */
export function useCarReviewsPage(
  reviewsLink: string | undefined,
  page: number,
  pageSize = CAR_REVIEWS_PAGE_SIZE,
) {
  return useQuery({
    queryKey: ['browse', 'reviews-page', reviewsLink, page, pageSize],
    enabled: !!reviewsLink,
    queryFn: async () => {
      const res = await getLinkCollectionPage<ReviewDto>(sessionClient, reviewsLink as string, {
        collectionAccept: MediaTypes.reviewLinks,
        itemAccept: MediaTypes.review,
        query: { page: page + 1, pageSize },
      });
      return { items: res.data ?? [], page: res.page };
    },
  });
}

/** Active bookable cars of the same category (server-side filter), excluding the current listing.
 *  Collection is link-only ({@code car.similar}); this hook follows each {@code self} — intentional HTTP N+1. */
export function useSimilarCars(car: CarDto | CarSummaryDto | undefined, limit = 4) {
  const similarLink = car?.links?.similar;
  return useQuery({
    queryKey: ['browse', 'similar', similarLink, limit],
    enabled: !!similarLink,
    queryFn: async () => {
      const res = await getLinkCollectionPage<CarSummaryDto>(sessionClient, similarLink as string, {
        collectionAccept: MediaTypes.carSimilar,
        itemAccept: MediaTypes.carSummary,
        query: { limit },
      });
      return res.data ?? [];
    },
  });
}

/** Admin: pausar/reactivar una publicación desde el detalle del auto (`PATCH /cars/{id}`). */
export function useAdminSetCarStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({
      carSelfLink,
      status,
    }: {
      carSelfLink: string;
      status: 'active' | 'admin_paused';
    }) =>
      sessionClient.patch<CarDto>(
        carSelfLink,
        { status },
        { accept: MediaTypes.car, contentType: MediaTypes.car },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['browse', 'car'] });
    },
  });
}

/** Minimal user fetch for review author names and owner avatar on car detail. */
export function useUserBrief(userLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'user-brief', userLink],
    enabled: !!userLink,
    queryFn: async () => {
      const res = await sessionClient.follow<UserDto>(userLink!, { accept: MediaTypes.user });
      return res.data;
    },
  });
}

export interface BookableSegmentDto {
  from: string;
  to: string;
  dayPrice: number | null;
  checkInTime: string | null;
  checkOutTime: string | null;
  location: string;
  neighborhoodId: number | null;
}

/** Rider bookable wall-day segments for the car-detail reservation picker. */
export function useCarBookableSegments(bookableSegmentsLink: string | undefined) {
  return useQuery({
    queryKey: ['browse', 'bookable-segments', bookableSegmentsLink],
    enabled: !!bookableSegmentsLink,
    queryFn: async () => {
      const res = await sessionClient.get<BookableSegmentDto[]>(bookableSegmentsLink!, {
        accept: MediaTypes.bookableSegment,
      });
      return res.data ?? [];
    },
  });
}
