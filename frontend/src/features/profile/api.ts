import { sessionClient } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { getLinkCollectionPage } from '../../api/client';
import { openAuthenticatedBinary } from '../../api/openAuthenticatedBinary';
import type { CarSummaryDto, DocumentType, UserDto, UserPatchDto } from './types';
import type { ReviewDto } from '../browse/types';

// =============================================================================
// Capa de acceso a la API para el área PROFILE.
// -----------------------------------------------------------------------------
// Se navega por hipervínculos cuando el DTO los trae (user.links.favorites,
// user.links.documents, etc.) y se cae a paths derivados de la URN del usuario
// (currentUserUri = "/users/{id}") cuando todavía no tenemos el DTO. Ningún
// path se "adivina"; se construye a partir de la URN canónica que da el server.
// =============================================================================

/** Deriva el path de un sub-recurso del usuario a partir de su URN. */
export function subResource(userUri: string, suffix: string): string {
  // userUri suele venir como "/users/5" (sin barra final).
  const base = userUri.endsWith('/') ? userUri.slice(0, -1) : userUri;
  return `${base}/${suffix}`;
}

/**
 * GET /users/{id} → UserDto. Por defecto pide la vista PÚBLICA (sirve para perfiles ajenos). Para el
 * perfil propio pasá `{ private: true }`: el server devuelve email/cbu/etc. bajo el media type privado
 * (solo self/admin; 403 si no corresponde). Son DOS representaciones, no el mismo type recortado.
 */
export async function fetchUser(userUri: string, opts?: { private?: boolean }): Promise<UserDto> {
  const accept = opts?.private ? MediaTypes.userPrivate : MediaTypes.user;
  const res = await sessionClient.get<UserDto>(userUri, { accept });
  return res.data;
}

/** PATCH /users/{id} con solo los campos cambiados (perfil propio → vista privada en la respuesta). */
export async function patchUser(userUri: string, patch: UserPatchDto): Promise<UserDto> {
  const res = await sessionClient.patch<UserDto>(userUri, patch, {
    accept: MediaTypes.userPrivate,
    contentType: MediaTypes.user,
  });
  return res.data;
}

/** PUT /users/{id}/profile-picture (raw image/* body). */
export async function uploadProfilePicture(user: UserDto, file: File): Promise<void> {
  const self = user.links?.self;
  const path = user.links?.profilePicture ?? (self ? subResource(self, 'profile-picture') : null);
  if (!path) throw new Error('profile.picture.missingLink');
  await sessionClient.request(path, {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

/** DELETE /users/{id}/profile-picture. */
export async function deleteProfilePicture(user: UserDto): Promise<void> {
  const self = user.links?.self;
  const path = user.links?.profilePicture ?? (self ? subResource(self, 'profile-picture') : null);
  if (!path) throw new Error('profile.picture.missingLink');
  await sessionClient.del(path);
}

/** Path de un documento (license/identity) — para subir, borrar y descargar. */
export function documentPath(user: UserDto, type: DocumentType): string {
  const self = user.links?.self;
  if (!self) throw new Error('profile.documents.missingLink');
  return `${subResource(self, 'documents')}/${type}`;
}

/** PUT /users/{id}/documents/{type} (raw octet-stream body). */
export async function uploadDocument(
  user: UserDto,
  type: DocumentType,
  file: File,
): Promise<void> {
  await sessionClient.request(documentPath(user, type), {
    method: 'PUT',
    body: file,
    contentType: file.type || 'application/octet-stream',
  });
}

/** DELETE /users/{id}/documents/{type}. */
export async function deleteDocument(user: UserDto, type: DocumentType): Promise<void> {
  await sessionClient.del(documentPath(user, type));
}

/**
 * Abre un documento (license/identity) en una pestaña nueva.
 *
 * El GET del documento exige `Authorization` (self/admin) y devuelve binario
 * (octet-stream / image / pdf). Un `<a href>` plano NO manda el Bearer token
 * (auth stateless por header, no cookies) → daría 401/403. Por eso replicamos
 * el viewer del JSP viejo: se baja el binario autenticado (vía el cliente
 * central, con el mismo reintento 401→refresh que cualquier otro request), se
 * crea un blob URL y se abre en una pestaña nueva. Devuelve false si falla
 * (p.ej. no subido, o el navegador bloqueó el popup).
 */
export async function openDocument(user: UserDto, type: DocumentType): Promise<boolean> {
  return openAuthenticatedBinary(documentPath(user, type));
}

/** Path de la colección de favoritos del usuario. */
export function favoritesPath(user: UserDto): string {
  const self = user.links?.self;
  const path = user.links?.favorites ?? (self ? subResource(self, 'favorites') : null);
  if (!path) throw new Error('profile.favorites.missingLink');
  return path;
}

/**
 * GET favoritos (paginados). `pathOrLink` puede ser el path base o un link de
 * paginación (next/prev) que ya trae los query params.
 */
export async function fetchFavorites(pathOrLink: string) {
  const res = await getLinkCollectionPage<CarSummaryDto>(sessionClient, pathOrLink, {
    collectionAccept: MediaTypes.userFavorites,
    itemAccept: MediaTypes.carSummary,
  });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** GET favoritos por índice de página (0-based URL, API 1-based). */
export async function fetchFavoritesPage(user: UserDto, pageIndex: number, pageSize?: number) {
  const res = await getLinkCollectionPage<CarSummaryDto>(sessionClient, favoritesPath(user), {
    collectionAccept: MediaTypes.userFavorites,
    itemAccept: MediaTypes.carSummary,
    query: { page: pageIndex + 1, pageSize },
  });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** DELETE /users/{id}/favorites/{carId} (idempotente). */
export async function removeFavorite(user: UserDto, carSelfLink: string): Promise<void> {
  // El carId sale del self del auto ("/cars/{id}").
  const carId = carSelfLink.split('/').filter(Boolean).pop();
  const base = favoritesPath(user);
  const normalized = base.endsWith('/') ? base.slice(0, -1) : base;
  await sessionClient.del(`${normalized}/${carId}`);
}

/** Path para listar los autos de un usuario (perfil público): GET /cars?ownerId=. */
export function userCarsLink(user: UserDto): string | null {
  return user.links?.cars ?? null;
}

/** GET autos activos de un usuario navegando user.links.cars. */
export async function fetchUserCars(carsLink: string) {
  const res = await sessionClient.get<CarSummaryDto[]>(carsLink, { accept: MediaTypes.carSummary });
  return { data: res.data ?? [], page: res.page, status: res.status };
}

/** Link a las reseñas recibidas por el usuario (GET /reviews?recipientUserId=). */
export function userReviewsLink(user: UserDto): string | null {
  return user.links?.reviews ?? null;
}

/**
 * GET reseñas recibidas navegando user.links.reviews (colección canónica).
 */
export async function getUserReviews(
  reviewsLink: string,
): Promise<{ data: ReviewDto[]; total: number }> {
  const res = await getLinkCollectionPage<ReviewDto>(sessionClient, reviewsLink, {
    collectionAccept: MediaTypes.reviewLinks,
    itemAccept: MediaTypes.review,
  });
  const data = res.data ?? [];
  return { data, total: res.page.total ?? data.length };
}
